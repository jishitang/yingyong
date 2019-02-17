/**
 * Copyright (C) @2014 Webank Group Holding Limited
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.webank.blockchain.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TimeZone;

/**
 * 简单封装Jackson，实现JSON String&lt;-&gt;Java Object的Mapper. 封装不同的输出风格,
 * 使用不同的builder函数创建实例.
 *
 * @author jonyang
 */
@SuppressWarnings("rawtypes")
public class JsonMapper {

    private static Logger logger = LoggerFactory.getLogger(JsonMapper.class);

    private ObjectMapper mapper;

    public JsonMapper() {
        this(null);
    }

    public JsonMapper(Include include) {
        mapper = new ObjectMapper();
        // 设置输出时包含属性的风格
        if (include != null) {
            mapper.setSerializationInclusion(include);
        }
        // 设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        //设置时区
        mapper.setTimeZone(TimeZone.getDefault());
    }

    /**
     * 创建只输出非Null且非Empty(如List.isEmpty)的属性到Json字符串的Mapper,建议在外部接口中使用.
     *
     * @return JsonMapper
     */
    public static JsonMapper nonEmptyMapper() {
        return new JsonMapper(Include.NON_NULL);
    }

    /**
     * 创建只输出初始值被改变的属性到Json字符串的Mapper, 最节约的存储方式，建议在内部接口中使用。
     *
     * @return JsonMapper
     */
    public static JsonMapper nonDefaultMapper() {
        return new JsonMapper(Include.NON_DEFAULT);
    }

    /**
     * Object可以是POJO，也可以是Collection或数组。 如果对象为Null, 返回"null". 如果集合为空集合, 返回"[]".
     *
     * @param object 对象
     * @return json串
     */
    public String toJson(Object object) {

        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            logger.warn("write to json string error:" + object, e);
            return null;
        }
    }

    /**
     * 用于打印json串，超过一定长度就过滤掉
     *
     * @param object
     * @param limit
     * @return
     */
    public String logJson(Object object, long limit) {
        String s = toJson(object);
        if (s != null && s.length() > limit) {
            return "filtered beause of exceed max length!";
        }
        return s;
    }

    /**
     * 反序列化POJO或简单Collection如List&lt;String&gt;. 如果JSON字符串为Null或"null"字符串,
     * 返回Null. 如果JSON字符串为"[]", 返回空集合. 如需反序列化复杂Collection如List&lt;MyBean&gt;,
     * 请使用fromJson(String, JavaType)
     *
     * @param jsonString json串
     * @param clazz      对象类型
     * @param <T>        返回对象类型
     * @return 对象
     * @see #fromJson(String, JavaType)
     */
    public <T> T fromJson(String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        try {
            return mapper.readValue(jsonString, clazz);
        } catch (IOException e) {
            logger.warn("parse json string error:" + jsonString, e);
            return null;
        }
    }

    /**
     * 反序列化复杂Collection如List&lt;Bean&gt;,
     * 先使用contructCollectionType()或contructMapType()构造类型, 然后调用本函数.
     *
     * @param jsonString json串
     * @param javaType   java类型
     * @param <T>        返回对象类型
     * @return 对象
     */
    @SuppressWarnings("unchecked")
    public <T> T fromJson(String jsonString, JavaType javaType) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        try {
            return (T) mapper.readValue(jsonString, javaType);
        } catch (IOException e) {
            logger.warn("parse json string error:" + jsonString, e);
            return null;
        }
    }

    /**
     * 构造Collection类型.
     *
     * @param collectionClass 集合类型
     * @param elementClass    元素类型
     * @return JavaType类型
     */
    public JavaType contructCollectionType(
            Class<? extends Collection> collectionClass, Class<?> elementClass) {
        return mapper.getTypeFactory().constructCollectionType(collectionClass,
                elementClass);
    }

    /**
     * 构造Map类型.
     *
     * @param mapClass   map类型
     * @param keyClass   key类型
     * @param valueClass value类型
     * @return JavaType类型
     */
    public JavaType contructMapType(Class<? extends Map> mapClass,
                                    Class<?> keyClass, Class<?> valueClass) {
        return mapper.getTypeFactory().constructMapType(mapClass, keyClass,
                valueClass);
    }

    /**
     * 当JSON里只含有Bean的部分属性时，更新一个已存在Bean，只覆盖部分的属性.
     *
     * @param jsonString json串
     * @param object     对象
     */
    public void update(String jsonString, Object object) {
        try {
            mapper.readerForUpdating(object).readValue(jsonString);
        } catch (JsonProcessingException e) {
            logger.warn("update json string:" + jsonString + " to object:"
                    + object + " error.", e);
        } catch (IOException e) {
            logger.warn("update json string:" + jsonString + " to object:"
                    + object + " error.", e);
        }
    }

    /**
     * 输出JSONP格式数据.
     *
     * @param functionName 功能名
     * @param object       对象
     * @return json串
     */
    public String toJsonP(String functionName, Object object) {
        return toJson(new JSONPObject(functionName, object));
    }

    /**
     * 设定是否使用Enum的toString函數來读写Enum,为False时使用Enum的name()函數來读写Enum, 默认为False.
     * 注意本函數一定要在Mapper创建后, 所有的读写动作之前调用.
     */
    public void enableEnumUseToString() {
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }

    /**
     * 支持使用Jaxb的Annotation，使得POJO上的annotation不用与Jackson耦合。
     * 默认会先查找jaxb的annotation，如果找不到再找jackson的。
     */
    public void enableJaxbAnnotation() {
        JaxbAnnotationModule module = new JaxbAnnotationModule();
        mapper.registerModule(module);
    }

    /**
     * 取出Mapper做进一步的设置或使用其他序列化API.
     *
     * @return ObjectMapper
     */
    public ObjectMapper getMapper() {
        return mapper;
    }
}

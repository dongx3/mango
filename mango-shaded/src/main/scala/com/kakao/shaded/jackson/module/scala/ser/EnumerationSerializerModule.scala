package com.kakao.shaded.jackson.module.scala.ser

import com.kakao.shaded.jackson.core.JsonGenerator

import com.kakao.shaded.jackson.databind._
import com.kakao.shaded.jackson.databind.ser.{ContextualSerializer, Serializers}

import com.kakao.shaded.jackson.module.scala.{JsonScalaEnumeration, JacksonModule}

import com.kakao.shaded.jackson.module.scala.util.Implicits._

trait ContextualEnumerationSerializer extends ContextualSerializer
{
  self: JsonSerializer[_] =>

  override def createContextual(serializerProvider: SerializerProvider, beanProperty: BeanProperty): JsonSerializer[_] =
    Option(beanProperty)
      .optMap(_.getAnnotation(classOf[JsonScalaEnumeration]))
      .map(_ => new AnnotatedEnumerationSerializer)
      .getOrElse(this)
}

/**
 * The implementation is taken from the code written by Greg Zoller, found here:
 * http://jira.codehaus.org/browse/JACKSON-211
 */
private class EnumerationSerializer extends JsonSerializer[scala.Enumeration#Value] with ContextualEnumerationSerializer {
	override def serialize(value: scala.Enumeration#Value, jgen: JsonGenerator, provider: SerializerProvider) = {
		val parentEnum = value.asInstanceOf[AnyRef].getClass.getSuperclass.getDeclaredFields.find( f => f.getName == "$outer" ).get
		val enumClass = parentEnum.get(value).getClass.getName stripSuffix "$"
		jgen.writeStartObject()
		jgen.writeStringField("enumClass", enumClass)
		jgen.writeStringField("value", value.toString)
		jgen.writeEndObject()
	}
}

private class AnnotatedEnumerationSerializer extends JsonSerializer[scala.Enumeration#Value] with ContextualEnumerationSerializer {
  override def serialize(value: scala.Enumeration#Value, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeString(value.toString)
  }
}

private object EnumerationSerializerResolver extends Serializers.Base {

  override def findSerializer(config: SerializationConfig,
                              javaType: JavaType,
                              beanDescription: BeanDescription): JsonSerializer[_] = {
		val clazz = javaType.getRawClass

    if (classOf[scala.Enumeration#Value].isAssignableFrom(clazz)) {
        new EnumerationSerializer
    } else {
      null
    }
	}

}

trait EnumerationSerializerModule extends JacksonModule {
  this += EnumerationSerializerResolver
}
package com.kakao.shaded.jackson.module.scala.deser

import java.util.AbstractMap
import java.util.Map.Entry

import scala.collection.{GenMap, mutable}
import scala.collection.immutable.HashMap

import com.kakao.shaded.jackson.core.JsonParser;

import com.kakao.shaded.jackson.databind._;
import com.kakao.shaded.jackson.databind.jsontype.{TypeDeserializer};

import com.kakao.shaded.jackson.databind.deser.std.{MapDeserializer, ContainerDeserializerBase};
import com.kakao.shaded.jackson.databind.`type`.MapLikeType;

import com.kakao.shaded.jackson.module.scala.modifiers.MapTypeModifierModule
import deser.{ContextualDeserializer, Deserializers, ValueInstantiator}

import scala.language.existentials

private class MapBuilderWrapper[K,V](val builder: mutable.Builder[(K,V), GenMap[K,V]]) extends AbstractMap[K,V] {
  override def put(k: K, v: V) = { builder += ((k,v)); v }

  // Isn't used by the deserializer
  def entrySet(): java.util.Set[Entry[K, V]] = throw new UnsupportedOperationException
}

private object UnsortedMapDeserializer {
  def builderFor(cls: Class[_]): mutable.Builder[(AnyRef,AnyRef), GenMap[AnyRef,AnyRef]] =
    if (classOf[HashMap[_,_]].isAssignableFrom(cls)) HashMap.newBuilder[AnyRef,AnyRef] else
    if (classOf[mutable.HashMap[_,_]].isAssignableFrom(cls)) mutable.HashMap.newBuilder[AnyRef,AnyRef] else
    if (classOf[mutable.ListMap[_,_]].isAssignableFrom(cls)) mutable.ListMap.newBuilder[AnyRef,AnyRef] else
    if (classOf[mutable.LinkedHashMap[_,_]].isAssignableFrom(cls)) mutable.LinkedHashMap.newBuilder[AnyRef,AnyRef] else
    if (classOf[mutable.Map[_,_]].isAssignableFrom(cls)) mutable.Map.newBuilder[AnyRef,AnyRef] else
    Map.newBuilder[AnyRef,AnyRef]
}

private class UnsortedMapDeserializer(
    collectionType: MapLikeType,
    config: DeserializationConfig,
    keyDeser: KeyDeserializer,
    valueDeser: JsonDeserializer[_],
    valueTypeDeser: TypeDeserializer)

  extends ContainerDeserializerBase[GenMap[_,_]](config.constructType(classOf[UnsortedMapDeserializer]))
  with ContextualDeserializer {

  private val javaContainerType =
    config.getTypeFactory.constructMapLikeType(classOf[MapBuilderWrapper[_,_]], collectionType.getKeyType, collectionType.getContentType)

  private val instantiator =
    new ValueInstantiator {
      override def getValueTypeDesc = collectionType.getRawClass.getCanonicalName
      override def canCreateUsingDefault = true
      override def createUsingDefault(ctxt: DeserializationContext) =
        new MapBuilderWrapper[AnyRef,AnyRef](UnsortedMapDeserializer.builderFor(collectionType.getRawClass))
    }

  private val containerDeserializer =
    new MapDeserializer(javaContainerType,instantiator,keyDeser,valueDeser.asInstanceOf[JsonDeserializer[AnyRef]],valueTypeDeser)

  override def getContentType = containerDeserializer.getContentType

  override def getContentDeserializer = containerDeserializer.getContentDeserializer
  
  override def createContextual(ctxt: DeserializationContext, property: BeanProperty) =
    if (keyDeser != null && valueDeser != null) this
    else {
      val newKeyDeser = Option(keyDeser).getOrElse(ctxt.findKeyDeserializer(collectionType.getKeyType, property))
      val newValDeser = Option(valueDeser).getOrElse(ctxt.findContextualValueDeserializer(collectionType.getContentType, property))
      new UnsortedMapDeserializer(collectionType, config, newKeyDeser, newValDeser, valueTypeDeser)
    }

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): GenMap[_,_] = {
    containerDeserializer.deserialize(jp, ctxt) match {
      case wrapper: MapBuilderWrapper[_,_] => wrapper.builder.result()
    }
  }
}

private object UnsortedMapDeserializerResolver extends Deserializers.Base {

  private val MAP = classOf[collection.Map[_,_]]
  private val SORTED_MAP = classOf[collection.SortedMap[_,_]]
  
  override def findMapLikeDeserializer(theType: MapLikeType,
                              config: DeserializationConfig,
                              beanDesc: BeanDescription,
                              keyDeserializer: KeyDeserializer,
                              elementTypeDeserializer: TypeDeserializer,
                              elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = theType.getRawClass

    if (!MAP.isAssignableFrom(rawClass)) null
    else if (SORTED_MAP.isAssignableFrom(rawClass)) null
    else new UnsortedMapDeserializer(theType, config, keyDeserializer, elementDeserializer, elementTypeDeserializer)
  }
}

/**
 * @author Christopher Currie <ccurrie@impresys.com>
 */
trait UnsortedMapDeserializerModule extends MapTypeModifierModule {
  this += { _.addDeserializers(UnsortedMapDeserializerResolver) }
}

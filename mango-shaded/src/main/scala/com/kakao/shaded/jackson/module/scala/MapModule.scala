package com.kakao.shaded.jackson.module.scala

import com.kakao.shaded.jackson.module.scala.ser.MapSerializerModule
import com.kakao.shaded.jackson.module.scala.deser.{UnsortedMapDeserializerModule, SortedMapDeserializerModule}

/**
 * @author Christopher Currie <christopher@currie.com>
 */
trait MapModule extends MapSerializerModule with SortedMapDeserializerModule with UnsortedMapDeserializerModule {

}
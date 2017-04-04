package com.kakao.shaded.jackson.module.scala

import com.kakao.shaded.jackson.module.scala.deser.EitherDeserializerModule
import com.kakao.shaded.jackson.module.scala.ser.EitherSerializerModule

trait EitherModule extends EitherDeserializerModule with EitherSerializerModule
package io.dylemma.battle

import ResourceKey._
import language.implicitConversions
import Combattant._

class Combattant(params: CombattantParam*) extends HasResources with HasStats {

	lazy val level = {
		params collectFirst { case CombattantLevel(lv) => lv } getOrElse 1
	}

	lazy val resources = {
		val r = for {
			CombattantResource(key, max) <- params
		} yield key -> new Resource(max)
		r.toMap
	}

	lazy val stats = {
		val s = for {
			CombattantStat(key, value) <- params
		} yield key -> value
		s.toMap
	}

}

object Combattant {
	sealed trait CombattantParam
	case class CombattantResource(key: ResourceKey, max: Int) extends CombattantParam
	case class CombattantStat(key: StatKey, value: Int) extends CombattantParam

	implicit def resourceInt2CombattantParam(kv: (ResourceKey, Int)): CombattantParam =
		CombattantResource(kv._1, kv._2)

	implicit def statInt2CombattantParam(kv: (StatKey, Int)): CombattantParam =
		CombattantStat(kv._1, kv._2)

	case class CombattantLevel(level: Int) extends CombattantParam

	def level(lv: Int) = CombattantLevel(lv)
}
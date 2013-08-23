package io.dylemma.battle.old

/** Some entity in the battle, e.g. a Combattant, the environment, etc */
trait BattleActor
case class CombattantActor(combattant: Combattant) extends BattleActor
case object EnvironmentActor extends BattleActor

trait BattleAction {
	def user: BattleActor
	def target: BattleActor
	def priority: Int
}

trait BattleReactor {
	def react(action: BattleAction): ReactionModification
}

class ReactionModification(
	val alteration: List[BattleAction],
	val reaction: List[BattleReaction])

trait ActionAlteration {
	def alter(action: BattleAction): List[BattleAction]
}

sealed trait BattleReaction
case class NewActionReaction(action: BattleAction) extends BattleReaction
trait EventReaction extends BattleReaction

// TODO: some battlereactions are also new BattleActions, but some are just effects that are bound to happen

class Battlefield {

}

object BattleSystem {

	implicit val actionOrdering: Ordering[BattleAction] = Ordering.by { _.priority }

	//	def handle(q: PriorityQueue[BattleAction])
}
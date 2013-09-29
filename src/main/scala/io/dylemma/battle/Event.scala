package io.dylemma.battle

trait Event {
	def calculatePriority(mods: BattleModifiers): Priority
}

trait UnprioritizedEvent extends Event {
	def calculatePriority(mods: BattleModifiers) = Priority()
}

object Event {
	val fleePriority = 3
	val itemPriority = 2
	val skillPriority = 1
}

// Turn Begin+End Markers
case object TurnBegin extends Event with UnprioritizedEvent
case object TurnEnd extends Event with UnprioritizedEvent

// Damage and Healing
case class DamageResource(target: HasResources, resource: ResourceKey, damage: Damage) extends Event with UnprioritizedEvent
case class RestoreResource(target: HasResources, resource: ResourceKey, amount: Int) extends Event with UnprioritizedEvent

// Add+Remove Battle Modifiers
case class AddBattleModifier(mod: BattleModifier) extends Event with UnprioritizedEvent
case class RemoveBattleModifier(mod: BattleModifier) extends Event with UnprioritizedEvent

// Add+Remove Event Handlers
case class AddEventHandler(handler: EventHandler) extends Event with UnprioritizedEvent
case class RemoveEventHandler(handler: EventHandler) extends Event with UnprioritizedEvent

trait Item // TODO: implement

sealed trait Action
case class SkillUse(skill: Skill, target: Target) extends Action
case class ItemUse(item: Item, target: Target) extends Action
case object Flee extends Action

case class CombattantAction(combattant: Combattant, action: Action) extends Event {
	def calculatePriority(mods: BattleModifiers) = action match {
		case Flee => Priority(Event.fleePriority)
		case ItemUse(item: Item, target: Target) => Priority(Event.itemPriority)
		case SkillUse(skill: Skill, target: Target) => Priority(Event.skillPriority) #> skill.calculatePriority(combattant, target, mods)
	}
}
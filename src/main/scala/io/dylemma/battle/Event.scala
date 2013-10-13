package io.dylemma.battle

trait Event {
	def calculatePriority(battleground: Battleground): Priority
}

trait UnprioritizedEvent extends Event {
	def calculatePriority(battleground: Battleground) = Priority()
}

object Event {
	val fleePriority = 3
	val itemPriority = 2
	val skillPriority = 1
}

// Turn Begin+End Markers
case object TurnBegin extends UnprioritizedEvent
case object TurnEnd extends UnprioritizedEvent

// Damage and Healing
case class DamageResource(target: HasResources, resource: ResourceKey, damage: Damage) extends UnprioritizedEvent
case class RestoreResource(target: HasResources, resource: ResourceKey, amount: Int) extends UnprioritizedEvent

// Add+Remove Battle Modifiers
case class AddBattleModifier(mod: BattleModifier) extends UnprioritizedEvent
case class RemoveBattleModifier(mod: BattleModifier) extends UnprioritizedEvent

// Add+Remove Event Handlers
case class AddEventHandler(handler: EventHandler) extends UnprioritizedEvent
case class RemoveEventHandler(handler: EventHandler) extends UnprioritizedEvent

trait Item // TODO: implement

sealed trait Action
case class SkillUse(skill: Skill, target: Target) extends Action
case class ItemUse(item: Item, target: Target) extends Action
case object Flee extends Action

// Combattant takes an action: [skill|item|flee]
case class CombattantAction(combattant: Combattant, action: Action) extends Event {
	def calculatePriority(battleground: Battleground) = action match {
		case Flee => Priority(Event.fleePriority)
		case ItemUse(item: Item, target: Target) => Priority(Event.itemPriority)
		case SkillUse(skill: Skill, target: Target) => Priority(Event.skillPriority) #> skill.calculatePriority(combattant, target, battleground)
	}
}

// Combattant Dies
case class CombattantDowned(combattant: Combattant) extends UnprioritizedEvent
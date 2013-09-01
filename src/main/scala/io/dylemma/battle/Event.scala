package io.dylemma.battle

trait Event

case object TurnBegan extends Event
case object TurnEnded extends Event
case class EventProcessorAdded(p: EventProcessor) extends Event
case class EventProcessorRemoved(p: EventProcessor) extends Event

case class AboutToDamageResource(
	target: Target, resource: ResourceKey, damage: Damage, affiliation: Affiliation)
	extends Event

case class DamagedResource(
	target: Target, resource: ResourceKey, damage: Damage, affiliation: Affiliation)
	extends Event

case class AboutToRestoreResource(
	target: Target, resource: ResourceKey, amount: Int, affiliation: Affiliation)
	extends Event

case class RestoredResource(
	target: Target, resource: ResourceKey, amount: Int, affiliation: Affiliation)
	extends Event

case class TargetFainted(target: Target) extends Event

case class SkillUsed(skill: Skill, user: Combattant, target: Target) extends Event
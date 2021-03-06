package com.gpl.rpg.AndorsTrail.controller;

import com.gpl.rpg.AndorsTrail.context.ViewContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.AttackResult;
import com.gpl.rpg.AndorsTrail.model.ability.ActorConditionEffect;
import com.gpl.rpg.AndorsTrail.model.ability.ActorConditionType;
import com.gpl.rpg.AndorsTrail.model.ability.SkillCollection;
import com.gpl.rpg.AndorsTrail.model.ability.SkillInfo;
import com.gpl.rpg.AndorsTrail.model.actor.Actor;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.model.item.ItemTypeCollection;
import com.gpl.rpg.AndorsTrail.model.item.DropList.DropItem;
import com.gpl.rpg.AndorsTrail.util.ConstRange;

public final class SkillController {
	private final ViewContext view;
	private final WorldContext world;

	public SkillController(ViewContext view, WorldContext world) {
		this.view = view;
		this.world = world;
	}

	public void applySkillEffects(Player player) {
		player.attackChance += SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_CHANCE * player.getSkillLevel(SkillCollection.SKILL_WEAPON_CHANCE);
		player.damagePotential.addToMax(SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_DAMAGE_MAX * player.getSkillLevel(SkillCollection.SKILL_WEAPON_DMG));
		player.damagePotential.add(SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_DAMAGE_MIN * player.getSkillLevel(SkillCollection.SKILL_WEAPON_DMG), false);
		player.blockChance += SkillCollection.PER_SKILLPOINT_INCREASE_DODGE * player.getSkillLevel(SkillCollection.SKILL_DODGE);
		player.damageResistance += SkillCollection.PER_SKILLPOINT_INCREASE_BARKSKIN * player.getSkillLevel(SkillCollection.SKILL_BARKSKIN);
		if (player.hasCriticalSkillEffect()) {
			if (player.criticalSkill > 0) {
				player.criticalSkill += player.criticalSkill * SkillCollection.PER_SKILLPOINT_INCREASE_MORE_CRITICALS_PERCENT * player.getSkillLevel(SkillCollection.SKILL_MORE_CRITICALS) / 100;
			}
		}
		if (player.hasCriticalMultiplierEffect()) {
			player.criticalMultiplier += player.criticalMultiplier * SkillCollection.PER_SKILLPOINT_INCREASE_BETTER_CRITICALS_PERCENT * player.getSkillLevel(SkillCollection.SKILL_BETTER_CRITICALS) / 100;
		}
		view.actorStatsController.addActorMaxAP(player, SkillCollection.PER_SKILLPOINT_INCREASE_SPEED * player.getSkillLevel(SkillCollection.SKILL_SPEED), false);
		/*final int berserkLevel = player.getSkillLevel(Skills.SKILL_BERSERKER);
		if (berserkLevel > 0) {
			final int berserkHealth = player.health.max * Skills.BERSERKER_STARTS_AT_HEALTH_PERCENT / 100;
			if (player.health.current <= berserkHealth) {
				player.traits.attackChance += Skills.PER_SKILLPOINT_INCREASE_BERSERKER_WEAPON_CHANCE * berserkLevel;
				player.traits.damagePotential.addToMax(Skills.PER_SKILLPOINT_INCREASE_BERSERKER_WEAPON_DAMAGE_MAX * berserkLevel);
				player.traits.damagePotential.add(Skills.PER_SKILLPOINT_INCREASE_BERSERKER_WEAPON_DAMAGE_MIN * berserkLevel, false);
				player.traits.blockChance += Skills.PER_SKILLPOINT_INCREASE_BERSERKER_DODGE * berserkLevel;
			}
		}*/
	}
	
	public static int getDropChanceRollBias(DropItem item, Player player) {
		if (player == null) return 0;
		
		if (ItemTypeCollection.isGoldItemType(item.itemType.id)) {
			return getRollBias(item, player, SkillCollection.SKILL_COINFINDER, SkillCollection.PER_SKILLPOINT_INCREASE_COINFINDER_CHANCE_PERCENT);
		} else if (!item.itemType.isOrdinaryItem()) {
			return getRollBias(item, player, SkillCollection.SKILL_MAGICFINDER, SkillCollection.PER_SKILLPOINT_INCREASE_MAGICFINDER_CHANCE_PERCENT);
		} else {
			return 0;
		}
	}
	
	public static int getDropQuantityRollBias(DropItem item, Player player) {
		if (player == null) return 0;
		if (!ItemTypeCollection.isGoldItemType(item.itemType.id)) return 0;
		
		return getRollBias(item, player, SkillCollection.SKILL_COINFINDER, SkillCollection.PER_SKILLPOINT_INCREASE_COINFINDER_QUANTITY_PERCENT);
	}
	
	private static int getRollBias(DropItem item, Player player, int skill, int perSkillpointIncrease) {
		return getRollBias(item.chance, player, skill, perSkillpointIncrease);
	}
	
	private static int getRollBias(ConstRange chance, Player player, int skill, int perSkillpointIncrease) {
		int skillLevel = player.getSkillLevel(skill);
		if (skillLevel <= 0) return 0;
		return chance.current * skillLevel * perSkillpointIncrease / 100;
	}
	
	
	public static boolean canLevelupSkillWithQuest(Player player, SkillInfo skill) {
		final int playerSkillLevel = player.getSkillLevel(skill.id);
		if (skill.hasMaxLevel()) {
			if (playerSkillLevel >= skill.maxLevel) return false;
		}
		if (!skill.canLevelUpSkillTo(player, playerSkillLevel + 1)) return false;
		return true;
	}
	public static boolean canLevelupSkillManually(Player player, SkillInfo skill) {
		if (!player.hasAvailableSkillpoints()) return false;
		if (!canLevelupSkillWithQuest(player, skill)) return false;
		if (skill.levelupVisibility == SkillInfo.LEVELUP_TYPE_ONLY_BY_QUESTS) return false;
		if (skill.levelupVisibility == SkillInfo.LEVELUP_TYPE_FIRST_LEVEL_REQUIRES_QUEST) {
			if (!player.hasSkill(skill.id)) return false;
		}
		return true;
	}
	public void levelUpSkillManually(Player player, SkillInfo skill) {
		if (!canLevelupSkillManually(player, skill)) return;
		player.availableSkillIncreases -= 1;
		addSkillLevel(skill.id);
	}
	public void levelUpSkillByQuest(Player player, SkillInfo skill) {
		if (!canLevelupSkillWithQuest(player, skill)) return;
		addSkillLevel(skill.id);
	}

	public void addSkillLevel(int skillID) {
		Player player = world.model.player;
		player.skillLevels.put(skillID, player.skillLevels.get(skillID) + 1);
		view.actorStatsController.recalculatePlayerStats(player);
	}
	
	public static int getActorConditionEffectChanceRollBias(ActorConditionEffect effect, Player player) {
		if (effect.chance.isMax()) return 0;
		
		int result = 0;
		result += getActorConditionEffectChanceRollBiasFromResistanceSkills(effect, player);
		result += getActorConditionEffectChanceRollBias(effect, player, SkillCollection.SKILL_SHADOW_BLESS, SkillCollection.PER_SKILLPOINT_INCREASE_RESISTANCE_SHADOW_BLESS);
		return result;
	}
	
	private static int getActorConditionEffectChanceRollBiasFromResistanceSkills(ActorConditionEffect effect, Player player) {
		int skill;
		switch (effect.conditionType.conditionCategory) {
		case ActorConditionType.ACTORCONDITIONTYPE_MENTAL:
			skill = SkillCollection.SKILL_RESISTANCE_MENTAL; break;
		case ActorConditionType.ACTORCONDITIONTYPE_PHYSICAL_CAPACITY:
			skill = SkillCollection.SKILL_RESISTANCE_PHYSICAL_CAPACITY; break;
		case ActorConditionType.ACTORCONDITIONTYPE_BLOOD_DISORDER:
			skill = SkillCollection.SKILL_RESISTANCE_BLOOD_DISORDER; break;
		default:
			return 0;
		}
		
		return getActorConditionEffectChanceRollBias(effect, player, skill, SkillCollection.PER_SKILLPOINT_INCREASE_RESISTANCE_CHANCE_PERCENT);
	}
	
	private static int getActorConditionEffectChanceRollBias(ActorConditionEffect effect, Player player, int skill, int chanceIncreasePerSkillLevel) {
		// Note that the bias should be negative, making it less likely that the chance roll will succeed
		return getRollBias(effect.chance, player, skill, -chanceIncreasePerSkillLevel);
	}
	
	public static boolean rollForSkillChance(Player player, int skill, int chancePerSkillLevel) {
		int skillLevel = player.getSkillLevel(skill);
		if (skillLevel <= 0) return false;
		return Constants.roll100(chancePerSkillLevel * skillLevel);
	}
	private void addConditionToActor(Actor target, String conditionName, int magnitude, int duration) {
		ActorConditionType conditionType = world.actorConditionsTypes.getActorConditionType(conditionName);
		ActorConditionEffect effect = new ActorConditionEffect(conditionType, magnitude, duration, null);
		view.actorStatsController.applyActorCondition(target, effect);
	}
			
	public void applySkillEffectsFromPlayerAttack(AttackResult result, Monster monster) {
		if (!result.isHit) return;
		
		Player player = world.model.player;
		
		if (player.getAttackChance() - monster.getBlockChance() > SkillCollection.CONCUSSION_THRESHOLD) {
			if (rollForSkillChance(player, SkillCollection.SKILL_CONCUSSION, SkillCollection.PER_SKILLPOINT_INCREASE_CONCUSSION_CHANCE)) {
				addConditionToActor(monster, "concussion", 1, 5);
			}
		}
		
		if (result.isCriticalHit) {
			if (rollForSkillChance(player, SkillCollection.SKILL_CRIT2, SkillCollection.PER_SKILLPOINT_INCREASE_CRIT2_CHANCE)) {
				addConditionToActor(monster, "crit2", 1, 5);
			}
			
			if (rollForSkillChance(player, SkillCollection.SKILL_CRIT1, SkillCollection.PER_SKILLPOINT_INCREASE_CRIT1_CHANCE)) {
				addConditionToActor(monster, "crit1", 1, 5);
			}
		}
	}
	
	public void applySkillEffectsFromMonsterAttack(AttackResult result, Monster monster) {
		if (!result.isHit) {
			if (rollForSkillChance(world.model.player, SkillCollection.SKILL_TAUNT, SkillCollection.PER_SKILLPOINT_INCREASE_TAUNT_CHANCE)) {
				view.actorStatsController.changeActorAP(monster, -SkillCollection.TAUNT_AP_LOSS, false, false);
			}
		}
	}

	public static void applySkillEffectsFromItemProficiencies(Player player) {
	}

	public static void applySkillEffectsFromFightingStyles(Player player) {
	}

	public static boolean isDualWielding(ItemType mainHandItem, ItemType offHandItem) {
		return false;
	}
}

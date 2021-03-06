var ATEditor = (function(ATEditor, model) {

	function isWeaponCategory(itemCategory) {
		if (!itemCategory) { return false; }
		if (itemCategory.actionType != 2) { return false; }
		if (itemCategory.inventorySlot != 0) { return false; }
		return true;
	}
	function isWeaponType(obj) {
		return isWeaponCategory(obj.category);
	}
	
	function getItemCost(obj) {
		if (obj.hasManualPrice == 1) {
			return obj.baseMarketCost;
		}
		return calculateItemCost(obj);
	}
	
	function calculateItemCost(obj) {
		var v = function(i) { return i ? parseFloat(i) : 0; }
		var sgn = function(v) {
			if (v < 0) return -1;
			else if (v > 0) return 1;
			else return 0;
		}
		
		var averageHPBoost = (v(obj.useEffect.increaseCurrentHP.min) + v(obj.useEffect.increaseCurrentHP.max)) / 2;
		var costBoostHP = Math.round(0.1*sgn(averageHPBoost)*Math.pow(Math.abs(averageHPBoost), 2) + 3*averageHPBoost);
		var itemUsageCost = costBoostHP;
		
		
		var isWeapon = isWeaponType(obj);
		
		var equip_blockChance = v(obj.equipEffect.increaseBlockChance);
		var equip_attackChance = v(obj.equipEffect.increaseAttackChance);
		var equip_attackCost = v(obj.equipEffect.increaseAttackCost);
		var equip_damageResistance = v(obj.equipEffect.increaseDamageResistance);
		var equip_attackDamage_Min = v(obj.equipEffect.increaseAttackDamage.min);
		var equip_attackDamage_Max = v(obj.equipEffect.increaseAttackDamage.max);
		var equip_criticalChance = v(obj.equipEffect.increaseCriticalSkill);
		var equip_criticalMultiplier = v(obj.equipEffect.setCriticalMultiplier);
		var costBC = Math.round(3*Math.pow(Math.max(0,equip_blockChance), 2.5) + 28*equip_blockChance);
		var costAC = Math.round(0.4*Math.pow(Math.max(0,equip_attackChance), 2.5) - 6*Math.pow(Math.abs(Math.min(0,equip_attackChance)),2.7));
		var costAP = isWeapon ?
				Math.round(0.2*Math.pow(10/equip_attackCost, 8) - 25*equip_attackCost)
				: -3125 * equip_attackCost;
		var costDR = 1325*equip_damageResistance;
		var costDMG_Min = isWeapon ?
				Math.round(10*Math.pow(equip_attackDamage_Min, 2.5))
				:Math.round(10*Math.pow(equip_attackDamage_Min, 3) + equip_attackDamage_Min*80);
		var costDMG_Max = isWeapon ?
				Math.round(2*Math.pow(equip_attackDamage_Max, 2.1))
				:Math.round(2*Math.pow(equip_attackDamage_Max, 3) + equip_attackDamage_Max*20);
		var costCC = Math.round(2.2*Math.pow(equip_criticalChance, 3));
		var costCM = Math.round(50*Math.pow(Math.max(0, equip_criticalMultiplier), 2));
		var costCombat = costBC + costAC + costAP + costDR + costDMG_Min + costDMG_Max + costCC + costCM;
		
		var equip_boostMaxHP = v(obj.equipEffect.increaseMaxHP);
		var equip_boostMaxAP = v(obj.equipEffect.increaseMaxAP);
		var equip_moveCostPenalty = v(obj.equipEffect.increaseMoveCost);
		var costMaxHP = Math.round(30*Math.pow(Math.max(0,equip_boostMaxHP), 1.2) + 70*equip_boostMaxHP);
		var costMaxAP = Math.round(50*Math.pow(Math.max(0,equip_boostMaxAP), 3) + 750*equip_boostMaxAP);
		var costMovement = Math.round(510*Math.pow(Math.max(0,-equip_moveCostPenalty), 2.5) - 350*equip_moveCostPenalty);
		var itemEquipCost = costCombat + costMaxHP + costMaxAP + costMovement;
		
		if (!obj.hasEquipEffect) { itemEquipCost = 0; }
		if (!obj.hasUseEffect) { itemUsageCost = 0; }
		
		var result = itemEquipCost + itemUsageCost;
		if (result <= 0) { result = 1; }
		
		return result;
	}
	
	function setCategoryToObject(item, itemCategories) {
		if (_.isString(item.category)) {
			item.category = itemCategories.findById(item.category);
		}
	}
	
	function ItemController($scope, $routeParams) {
		$scope.obj = model.items.findById($routeParams.id) || {};
		$scope.itemCategories = model.itemCategories.items;
		setCategoryToObject($scope.obj, model.itemCategories);
		
		$scope.$watch('obj.category', function(val) {
			$scope.isWeapon = isWeaponCategory(val);
		});
		$scope.$watch('obj.hasManualPrice', function(hasManualPrice) {
			$scope.obj.baseMarketCost = hasManualPrice ? calculateItemCost($scope.obj) : 0;
		});
		
		$scope.getItemCost = getItemCost;
		$scope.getItemSellingCost = function(obj) {
			var cost = getItemCost(obj);
			return Math.round(cost * (100 + 15) / 100);
		};
		$scope.getItemBuyingCost = function(obj) {
			var cost = getItemCost(obj);
			return Math.round(cost * (100 - 15) / 100);
		};
		
		
		$scope.addCondition = function(list) {
			list.push({magnitude:1});
		};
		$scope.removeCondition = function(list, cond) {
			var idx = list.indexOf(cond);
			list.splice(idx, 1);
		};
	}
	
	function ItemTableController($scope, $routeParams) {
		$scope.items = model.items.items;
		$scope.itemCategories = model.itemCategories.items;
		_.each($scope.items, function(item) {
			setCategoryToObject(item, model.itemCategories);
		});
		$scope.getItemCost = getItemCost;
		$scope.edit = function(item) {
			window.location = "#/" + model.items.id + "/edit/" + item.id;
		};
		$scope.updateCost = function(item) {
			item.baseMarketCost = item.hasManualPrice ? calculateItemCost(item) : 0;
		};
		
		$scope.iconID = true;
		$scope.id = true;
		$scope.cost = true;
	}
	
	ATEditor.controllers = ATEditor.controllers || {};
	ATEditor.controllers.ItemController = ItemController;
	ATEditor.controllers.ItemTableController = ItemTableController;

	return ATEditor;
})(ATEditor, ATEditor.model);

1. 建筑间隔究竟是谁负责，`area_to_clear`？是没有生效还是另有其他对象负责？
  * `summoning_wand`召唤法杖进入建造模式以后，应该有调用相应字段
  * `org.millenaire.item.SummoningWandItem`
    * `ClearMargins margins = planSet.clearMargins().atLeast(slot.clearMargin());`
    * `clearMargin()`属于`VillageType`, 暂时不关心它
    * `clearMargins()`属于`BuildingPlanSet`, 返回一个类`org.millenaire.building.ClearMargins`
      * 来到`BuildingPlanSetLoader`查看`clearMargins`的来源
        * 那就是`area_to_clear`
          ```java
            int areaToClear = GsonHelper.getAsInt(json, "area_to_clear", 0);
            int atcLengthBefore = GsonHelper.getAsInt(json, "area_to_clear_length_before", -1);
            int atcLengthAfter = GsonHelper.getAsInt(json, "area_to_clear_length_after", -1);
            int atcWidthBefore = GsonHelper.getAsInt(json, "area_to_clear_width_before", -1);
            int atcWidthAfter = GsonHelper.getAsInt(json, "area_to_clear_width_after", -1);
            ClearMargins clearMargins = ClearMargins.fromLegacy(areaToClear, atcLengthBefore, atcLengthAfter, atcWidthBefore, atcWidthAfter);
          ```
        * 那为什么改它没用？显然`VillageType`是第一嫌疑人
        * `VillageTypeLoader`查找`clearMargins`
          * `int clearMargin = GsonHelper.getAsInt(slot, "clear_margin", MillenaireServerConfig.getDefaultClearMargin());`
        * 双声明，那么`atLeast`就很明确了
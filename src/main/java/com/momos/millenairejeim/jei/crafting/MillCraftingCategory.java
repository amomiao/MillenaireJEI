// region MillCraftingCategory 全量代码实现
package com.momos.millenairejeim.jei.crafting;

import com.mojang.blaze3d.platform.InputConstants;
import com.momos.millenairejeim.gui.widget.MillPlusButtonWidget;
import com.momos.millenairejeim.helper.MillenaireLocalizeHelper;
import com.momos.millenairejeim.jei.MillenaireJeimKeys;
import com.momos.millenairejeim.jei.crafting.event.MillRecipeButtonClickEvent;
import com.momos.millenairejeim.jei.crafting.event.MillRecipeButtonRenderEvent;
import com.momos.millenairejeim.jei.crafting.event.MillRecipeSetSlotEvent;
import com.momos.millenairejeim.jei.crafting.type.base.IMillRecipe;
import com.momos.millenairejeim.util.MillenaireJeimLocalizeHelper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;

import java.util.Arrays;
import java.util.List;

/**
 * 千年工艺 JEI GUI 界面绘制与分类定义[cite: 15]。
 * 重新由 Category 统一负责背景、槽位、动画及文本和 Tooltip 的全量渲染[cite: 15]。
 */
public class MillCraftingCategory implements IRecipeCategory<IMillRecipe> {
    public static final String BUTTON_ID_VILLAGER_DETAIL = "villager_detail_plus";
    private static final int VILLAGER_BUTTON_X = 142;
    private static final int VILLAGER_BUTTON_Y = 54;

    private final RecipeType<IMillRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;

    // GUI 基础纹理组件
    private final IDrawable inputSlotDrawable;
    private final IDrawable outputSlotDrawable;
    private final IDrawableAnimated animatedArrow;

    /* =========================================================================================
     * [醒目新注释] 实例化控件：通用加号按钮控件
     * ========================================================================================= */
    private final MillPlusButtonWidget villagerPlusButton = new MillPlusButtonWidget(12, 12);

    public MillCraftingCategory(IGuiHelper guiHelper, RecipeType<IMillRecipe> recipeType, Component title, ItemStack iconStack) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(iconStack);

        this.inputSlotDrawable = guiHelper.getSlotDrawable();
        this.outputSlotDrawable = guiHelper.getOutputSlot();
        this.animatedArrow = guiHelper.createAnimatedRecipeArrow(200);
    }

    public IDrawable getInputSlotDrawable() {
        return inputSlotDrawable;
    }

    public IDrawable getOutputSlotDrawable() {
        return outputSlotDrawable;
    }

    public IDrawableAnimated getAnimatedArrow() {
        return animatedArrow;
    }

    @Override
    public RecipeType<IMillRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 160;
    }

    @Override
    public int getHeight() {
        return 105;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IMillRecipe recipe, IFocusGroup focuses) {
        List<IMillRecipe.IngredientWithCount> inputs = recipe.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            int x = 6 + (i % 3) * 18;
            int y = 6 + (i / 3) * 18;
            IMillRecipe.IngredientWithCount input = inputs.get(i);

            ItemStack[] displayStacks = Arrays.stream(input.ingredient().getItems())
                    .map(stack -> {
                        ItemStack copy = stack.copy();
                        copy.setCount(input.count());
                        return copy;
                    })
                    .toArray(ItemStack[]::new);

            builder.addSlot(RecipeIngredientRole.INPUT, x + 1, y + 1)
                    .addIngredients(VanillaTypes.ITEM_STACK, Arrays.asList(displayStacks));
        }

        List<ItemStack> outputs = recipe.getOutputs();
        for (int i = 0; i < outputs.size() && i < 2; i++) {
            int x = 108;
            int y = (outputs.size() == 1) ? 15 : (6 + i * 18);

            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                    .addItemStack(outputs.get(i));
        }

        List<ItemStack> itemCatalysts = recipe.getCatalysts();
        if (itemCatalysts != null && !itemCatalysts.isEmpty()) {
            for (int i = 0; i < itemCatalysts.size(); i++) {
                builder.addSlot(RecipeIngredientRole.CATALYST, 60 + (i * 18), 38)
                        .addItemStack(itemCatalysts.get(i));
            }
        }

        NeoForge.EVENT_BUS.post(new MillRecipeSetSlotEvent(builder, recipe, focuses));
    }

    @Override
    public void draw(IMillRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        if (recipe == null) return;

        Font font = Minecraft.getInstance().font;

        // 1. 绘制 6 个输入槽位背景 (3x2 布局)[cite: 15]
        if (inputSlotDrawable != null) {
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 3; col++) {
                    inputSlotDrawable.draw(guiGraphics, 6 + col * 18, 6 + row * 18);
                }
            }
        }

        // 2. 绘制进度动画箭头[cite: 15]
        if (animatedArrow != null) {
            animatedArrow.draw(guiGraphics, 72, 15);
        }

        // 3. 构建制作类型文本（通过 handlerId 判断类型，兼容多态显示）[cite: 15]
        String handlerId = recipe.getHandlerId() != null ? recipe.getHandlerId().toLowerCase() : "crafting";
        Component localizedCraftingType = MillenaireLocalizeHelper.getCraftingTypeName(handlerId);
        String rawGoalKey = recipe.getGoalKey() != null ? recipe.getGoalKey() : (recipe.getId() != null ? recipe.getId().getPath() : "unknown");
        Component localizedGoalName = MillenaireLocalizeHelper.getGoalName(rawGoalKey);
        String formattedCraftingType = localizedCraftingType.getString() + " - " + localizedGoalName.getString();

        // 4. 构建制作村民汇总文本[cite: 15]
        List<VillagerType> villagers = recipe.getVillagerTypes();
        String villagerSummary;
        if (villagers == null || villagers.isEmpty()) {
            villagerSummary = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_NONE_VILLAGER,
                    MillenaireJeimKeys.FALLBACK_NONE_VILLAGER
            ).getString();
        } else {
            String firstVillagerName = MillenaireJeimLocalizeHelper.getCraftingVillagerText(villagers.get(0)).getString();
            if (villagers.size() > 1) {
                String countSuffix = Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_COUNT_PEOPLE,
                        MillenaireJeimKeys.FALLBACK_COUNT_PEOPLE,
                        villagers.size()
                ).getString();
                villagerSummary = firstVillagerName + " " + countSuffix;
            } else {
                villagerSummary = firstVillagerName;
            }
        }

        // 5. 构建关联建筑汇总文本[cite: 15]
        List<BuildingPlanSet> buildings = recipe.getAssociatedBuildingIds();
        String buildingSummary;
        if (buildings == null || buildings.isEmpty()) {
            buildingSummary = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_NONE_BUILDING,
                    MillenaireJeimKeys.FALLBACK_NONE_BUILDING
            ).getString();
        } else {
            String firstBuildingName = MillenaireJeimLocalizeHelper.getCraftingBuildingText(buildings.get(0)).getString();
            if (buildings.size() > 1) {
                String countSuffix = Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_COUNT_PLACES,
                        MillenaireJeimKeys.FALLBACK_COUNT_PLACES,
                        buildings.size()
                ).getString();
                buildingSummary = firstBuildingName + " " + countSuffix;
            } else {
                buildingSummary = firstBuildingName;
            }
        }

        // 6. 获取固定标签文本[cite: 15]
        String labelCraftingType = Component.translatableWithFallback(
                MillenaireJeimKeys.KEY_LABEL_CRAFTING_TYPE,
                MillenaireJeimKeys.FALLBACK_LABEL_CRAFTING_TYPE
        ).getString();
        String labelVillager = Component.translatableWithFallback(
                MillenaireJeimKeys.KEY_LABEL_VILLAGER,
                MillenaireJeimKeys.FALLBACK_LABEL_VILLAGER
        ).getString();
        String labelBuilding = Component.translatableWithFallback(
                MillenaireJeimKeys.KEY_LABEL_BUILDING,
                MillenaireJeimKeys.FALLBACK_LABEL_BUILDING
        ).getString();

        // 7. 渲染文本内容[cite: 15]
        guiGraphics.drawString(font, "§8" + labelCraftingType, 6, 45, 0x404040, false);
        guiGraphics.drawString(font, "§5" + formattedCraftingType, 12, 54, 0x404040, false);

        guiGraphics.drawString(font, "§8" + labelVillager, 6, 65, 0x404040, false);
        guiGraphics.drawString(font, "§2" + villagerSummary, 12, 74, 0x404040, false);

        guiGraphics.drawString(font, "§8" + labelBuilding, 6, 85, 0x404040, false);
        guiGraphics.drawString(font, "§3" + buildingSummary, 12, 94, 0x404040, false);

        /* =========================================================================================
         * [醒目新注释] 事件1应用：询问是否绘制【村民详情查看 (+) 按钮】
         * ========================================================================================= */
        boolean defaultShouldRender = villagers != null && !villagers.isEmpty();
        MillRecipeButtonRenderEvent renderEvent = new MillRecipeButtonRenderEvent(recipe, BUTTON_ID_VILLAGER_DETAIL, defaultShouldRender);
        renderEvent.setShouldRender(false);
        NeoForge.EVENT_BUS.post(renderEvent);
        if (renderEvent.isShouldRender()) {
            villagerPlusButton.render(guiGraphics, font, VILLAGER_BUTTON_X, VILLAGER_BUTTON_Y, mouseX, mouseY);
        }
    }

    /**
     * [醒目新注释] JEI 19.x+ 声明式输入处理器挂载方法。
     * 使用正确包路径的 {@link IRecipeExtrasBuilder} 与实现 {@link ScreenRectangle} 的 {@link IJeiInputHandler} 匿名类。
     *
     * @param builder  JEI 额外 UI 元素与输入构建器
     * @param recipe   当前渲染的千年工艺配方
     * @param focuses  当前 JEI 焦点组
     */
    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, IMillRecipe recipe, IFocusGroup focuses) {
        boolean defaultShouldRender = recipe.getVillagerTypes() != null && !recipe.getVillagerTypes().isEmpty();
        MillRecipeButtonRenderEvent renderEvent = new MillRecipeButtonRenderEvent(recipe, BUTTON_ID_VILLAGER_DETAIL, defaultShouldRender);
        NeoForge.EVENT_BUS.post(renderEvent);

        if (renderEvent.isShouldRender()) {
            builder.addInputHandler(new IJeiInputHandler() {
                /**
                 * [醒目新注释] 准确定义按钮在 JEI 配方区域内的相对矩形响应范围。
                 */
                @Override
                public ScreenRectangle getArea() {
                    return new ScreenRectangle(VILLAGER_BUTTON_X, VILLAGER_BUTTON_Y, villagerPlusButton.getWidth(), villagerPlusButton.getHeight());
                }

                /**
                 * [醒目新注释] 处理输入响应。
                 * 移除多余的 isHovered 校验，点击直接向 {@link NeoForge#EVENT_BUS} 发送事件。
                 */
                @Override
                public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
                    InputConstants.Key key = input.getKey();
                    if (key.getType() == InputConstants.Type.MOUSE && key.getValue() == 0) { // 0 代表鼠标左键
                        NeoForge.EVENT_BUS.post(new MillRecipeButtonClickEvent(recipe, BUTTON_ID_VILLAGER_DETAIL, mouseX, mouseY, 0));
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, IMillRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (recipe == null) return;

        if (mouseX >= 6 && mouseX <= 154) {
            if (mouseY >= 65 && mouseY < 83) {
                List<VillagerType> villagers = recipe.getVillagerTypes();
                if (villagers != null && villagers.size() > 1) {
                    tooltip.add(Component.translatableWithFallback(
                            MillenaireJeimKeys.KEY_TOOLTIP_VILLAGERS_HEADER,
                            MillenaireJeimKeys.FALLBACK_TOOLTIP_VILLAGERS_HEADER
                    ));
                    villagers.forEach(v -> {
                        Component vName = MillenaireJeimLocalizeHelper.getCraftingVillagerText(v);
                        MillenaireJeimLocalizeHelper.addTooltipEntry(tooltip, vName);
                    });
                }
                return;
            }

            if (mouseY >= 85 && mouseY <= 103) {
                List<BuildingPlanSet> buildings = recipe.getAssociatedBuildingIds();
                if (buildings != null && buildings.size() > 1) {
                    tooltip.add(Component.translatableWithFallback(
                            MillenaireJeimKeys.KEY_TOOLTIP_BUILDINGS_HEADER,
                            MillenaireJeimKeys.FALLBACK_TOOLTIP_BUILDINGS_HEADER
                    ));
                    buildings.forEach(b -> {
                        Component bName = MillenaireJeimLocalizeHelper.getCraftingBuildingText(b);
                        MillenaireJeimLocalizeHelper.addTooltipEntry(tooltip, bName);
                    });
                }
            }
        }
    }
}
// endregion MillCraftingCategory 全量代码实现
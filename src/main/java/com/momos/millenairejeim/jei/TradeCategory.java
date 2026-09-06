package com.momos.millenairejeim.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 交易 Category 渲染器。
 * 负责渲染 {@link TradeRecipe} 的输入输出槽位、商店信息以及声望限制提示。
 */
public class TradeCategory implements IRecipeCategory<TradeRecipe> {
    private final RecipeType<TradeRecipe> recipeType;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;
    private final boolean isSellingCategory;

    public TradeCategory(IGuiHelper guiHelper, RecipeType<TradeRecipe> recipeType, Component title, ItemStack iconStack, boolean isSellingCategory) {
        this.recipeType = recipeType;
        this.title = title;
        this.isSellingCategory = isSellingCategory;
        this.background = guiHelper.createBlankDrawable(160, 60);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<TradeRecipe> getRecipeType() {
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
        return 60;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TradeRecipe recipe, IFocusGroup focuses) {
        if (isSellingCategory) {
            // 千年售出：输入货币，输出商品
            for (int i = 0; i < recipe.getCoinStacks().size(); i++) {
                builder.addSlot(RecipeIngredientRole.INPUT, 10 + (i * 18), 30)
                        .addItemStack(recipe.getCoinStacks().get(i));
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 30)
                    .addIngredients(recipe.getItemIngredient());
        } else {
            // 千年购入：输入商品，输出货币
            builder.addSlot(RecipeIngredientRole.INPUT, 10, 30)
                    .addIngredients(recipe.getItemIngredient());
            for (int i = 0; i < recipe.getCoinStacks().size(); i++) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 80 + (i * 18), 30)
                        .addItemStack(recipe.getCoinStacks().get(i));
            }
        }
    }

    @Override
    public void draw(TradeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        // 渲染文化与商店来源
        String shopInfo = recipe.getCultureId().getPath() + " - " + recipe.getShopId();
        guiGraphics.drawString(font, shopInfo, 5, 5, 0x404040, false);

        // 若为可选购入，标记提示
        if (recipe.getTradeType() == TradeRecipe.TradeType.VILLAGE_BUYS_OPTIONAL) {
            guiGraphics.drawString(font, "(次要收购)", 100, 5, 0x888888, false);
        }

        // 若存在最低声望要求，进行文本渲染提示
        if (recipe.getTradeGood().minReputation() > 0) {
            String repInfo = "需要声望: " + recipe.getTradeGood().minReputation();
            guiGraphics.drawString(font, repInfo, 5, 48, 0xAA0000, false);
        }
    }
}
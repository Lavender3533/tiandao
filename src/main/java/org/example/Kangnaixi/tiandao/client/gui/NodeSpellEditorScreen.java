package org.example.Kangnaixi.tiandao.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.example.Kangnaixi.tiandao.item.SpellJadeSlipItem;
import org.example.Kangnaixi.tiandao.spell.node.NodeSpell;
import org.example.Kangnaixi.tiandao.spell.node.SpellNode;
import org.example.Kangnaixi.tiandao.spell.node.TriggerType;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellExecutor;
import org.example.Kangnaixi.tiandao.spell.node.TriggerType;

/**
 * Lightweight placeholder for the future drag-and-drop editor.
 * Provides basic spell info editing so the rest of the pipeline can compile and run.
 */
public class NodeSpellEditorScreen extends Screen {

    private final NodeSpell spell;
    private EditBox nameInput;
    private Button triggerButton;

    public NodeSpellEditorScreen() {
        this(new NodeSpell("new_spell", "New Spell"));
    }

    public NodeSpellEditorScreen(NodeSpell spell) {
        super(Component.literal("Node Spell Editor"));
        this.spell = spell;
        if (spell.getNodes().isEmpty()) {
            spell.addNode(new SpellNode(1, 1));
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        nameInput = new EditBox(this.font, centerX - 100, 30, 200, 20, Component.literal("Spell Name"));
        nameInput.setValue(spell.getName());
        this.addRenderableWidget(nameInput);

        triggerButton = Button.builder(
            Component.literal("Trigger: " + spell.getTriggerType().getDisplayName()),
            btn -> cycleTrigger())
            .bounds(centerX - 100, 60, 200, 20)
            .build();
        this.addRenderableWidget(triggerButton);

        this.addRenderableWidget(Button.builder(Component.literal("Add Node"), btn -> addNode())
            .bounds(centerX - 100, 90, 95, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Clear"), btn -> clearNodes())
            .bounds(centerX + 5, 90, 95, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Test"), btn -> testSpell())
            .bounds(centerX - 100, 120, 95, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> saveSpell())
            .bounds(centerX + 5, 120, 95, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
            .bounds(centerX - 50, 150, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = this.width / 2 - 110;
        graphics.drawString(this.font, Component.literal("Nodes: " + spell.getNodes().size()), x, 190, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal(
            "Total Cost: " + String.format("%.1f", spell.calculateTotalSpiritCost())), x, 205, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal(
            "Cooldown: " + String.format("%.1f s", spell.calculateCooldown())), x, 220, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal("Use CMD /nodeeditor for full editor (WIP)"),
            x, this.height - 30, 0xAAAAAA);
    }

    private void cycleTrigger() {
        TriggerType[] types = TriggerType.values();
        int next = (spell.getTriggerType().ordinal() + 1) % types.length;
        spell.setTriggerType(types[next]);
        triggerButton.setMessage(Component.literal("Trigger: " + spell.getTriggerType().getDisplayName()));
    }

    private void addNode() {
        int idx = spell.getNodes().size() + 1;
        spell.addNode(new SpellNode(idx, 1));
    }

    private void clearNodes() {
        spell.getNodes().clear();
        addNode();
    }

    private void testSpell() {
        if (this.minecraft == null || this.minecraft.player == null) return;
        spell.setName(nameInput.getValue());
        NodeSpell.ValidationResult validation = spell.validate();
        if (!validation.isValid()) {
            this.minecraft.player.displayClientMessage(Component.literal("Validation failed: " + validation.getMessage()), false);
            return;
        }
        NodeSpellExecutor.ExecutionResult result = NodeSpellExecutor.getInstance().execute(spell, this.minecraft.player);
        this.minecraft.player.displayClientMessage(
            Component.literal(result.isSuccess() ? "Spell executed." : "Execution failed: " + result.getMessage()),
            false);
    }

    private void saveSpell() {
        if (this.minecraft == null || this.minecraft.player == null) return;
        spell.setName(nameInput.getValue());
        NodeSpell.ValidationResult validation = spell.validate();
        if (!validation.isValid()) {
            this.minecraft.player.displayClientMessage(Component.literal("Save failed: " + validation.getMessage()), false);
            return;
        }
        ItemStack slip = SpellJadeSlipItem.createNodeSpellSlip(spell);
        if (!this.minecraft.player.addItem(slip)) {
            this.minecraft.player.drop(slip, false);
        }
        this.minecraft.player.displayClientMessage(Component.literal("Spell stored in jade slip."), false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

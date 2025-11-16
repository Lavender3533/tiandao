package org.example.Kangnaixi.tiandao.spell.node.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.node.NodeSpell;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellContext;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellExecutor;
import org.example.Kangnaixi.tiandao.spell.node.projectile.ProjectileTemplate;
import org.example.Kangnaixi.tiandao.spell.node.projectile.ProjectileTemplateRegistry;
import org.example.Kangnaixi.tiandao.spell.node.target.SpellTargetSet;

public class NodeSpellProjectileEntity extends ThrowableProjectile {

    private static final EntityDataAccessor<String> DATA_SPELL_ID =
        SynchedEntityData.defineId(NodeSpellProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_TEMPLATE_ID =
        SynchedEntityData.defineId(NodeSpellProjectileEntity.class, EntityDataSerializers.STRING);

    private NodeSpell spell;
    private int startNodeIndex;
    private boolean initialized;
    private ProjectileTemplate template;

    public NodeSpellProjectileEntity(EntityType<? extends NodeSpellProjectileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SPELL_ID, "");
        this.entityData.define(DATA_TEMPLATE_ID, "tiandao:projectile/basic");
    }

    public void setSpellData(NodeSpell spell, int startNodeIndex) {
        this.spell = spell;
        this.startNodeIndex = startNodeIndex;
        this.initialized = true;
        if (spell != null) {
            this.entityData.set(DATA_SPELL_ID, spell.getId());
        }
    }

    public void setTemplateId(String templateId) {
        this.entityData.set(DATA_TEMPLATE_ID, templateId);
        this.template = ProjectileTemplateRegistry.getInstance().get(templateId);
        if (this.template == null) {
            Tiandao.LOGGER.warn("Unknown projectile template {}, using default", templateId);
            this.template = ProjectileTemplateRegistry.getInstance().get("tiandao:projectile/basic");
        }
    }

    private ProjectileTemplate template() {
        if (template == null) {
            setTemplateId(this.entityData.get(DATA_TEMPLATE_ID));
        }
        return template;
    }

    @Override
    public void tick() {
        super.tick();
        ProjectileTemplate tpl = template();
        if (this.tickCount > tpl.getLifetime()) {
            discard();
            return;
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(resolveParticle(tpl.getTrailParticle()),
                getX(), getY(), getZ(), 1, 0, 0, 0, 0.0);
        }
    }

    @Override
    protected float getGravity() {
        return (float) template().getGravity();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) {
            return;
        }
        if (!initialized || spell == null) {
            Tiandao.LOGGER.warn("NodeSpellProjectile hit without initialized spell");
            discard();
            return;
        }
        if (!(getOwner() instanceof net.minecraft.world.entity.player.Player player)) {
            Tiandao.LOGGER.warn("NodeSpellProjectile has no player owner");
            discard();
            return;
        }

        NodeSpellContext context = new NodeSpellContext(player);
        context.setPosition(this.position());
        context.setDirection(this.getDeltaMovement().normalize());
        SpellTargetSet targets = new SpellTargetSet();
        targets.addPointTarget(this.position());
        targets.setSource(this);
        context.replaceTargets(targets);

        NodeSpellExecutor.getInstance().executeRemaining(spell, context, startNodeIndex);
        discard();
    }

    private ParticleOptions resolveParticle(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc != null) {
            var type = BuiltInRegistries.PARTICLE_TYPE.getOptional(loc).orElse(null);
            if (type instanceof SimpleParticleType simple) {
                return simple;
            }
        }
        return ParticleTypes.END_ROD;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (spell != null) {
            tag.put("Spell", spell.toNBT());
        }
        tag.putInt("StartNodeIndex", startNodeIndex);
        tag.putBoolean("Initialized", initialized);
        tag.putString("TemplateId", this.entityData.get(DATA_TEMPLATE_ID));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Spell")) {
            this.spell = NodeSpell.fromNBT(tag.getCompound("Spell"));
        }
        this.startNodeIndex = tag.getInt("StartNodeIndex");
        this.initialized = tag.getBoolean("Initialized");
        if (tag.contains("TemplateId")) {
            setTemplateId(tag.getString("TemplateId"));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

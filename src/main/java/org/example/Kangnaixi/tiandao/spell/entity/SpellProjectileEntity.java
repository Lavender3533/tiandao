package org.example.Kangnaixi.tiandao.spell.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneChainExecutor;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;
import org.example.Kangnaixi.tiandao.spell.rune.RuneRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 术法弹道实体
 * 携带符文链，命中时执行效果
 */
public class SpellProjectileEntity extends ThrowableProjectile {

    // 符文ID列表（用于网络同步）
    private static final EntityDataAccessor<String> DATA_RUNE_IDS =
        SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.STRING);

    // 本地符文链缓存
    private List<Rune> runeChain = new ArrayList<>();
    private boolean runesInitialized = false;

    public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RUNE_IDS, "");
    }

    /**
     * 设置符文链
     */
    public void setRuneChain(List<Rune> runes) {
        this.runeChain = new ArrayList<>(runes);
        this.runesInitialized = true;

        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < runes.size(); i++) {
            if (i > 0) ids.append(",");
            ids.append(runes.get(i).getId());
        }
        this.entityData.set(DATA_RUNE_IDS, ids.toString());
    }

    /**
     * 从网络数据重建符文链
     */
    private void rebuildRuneChain() {
        if (runesInitialized) return;

        String ids = this.entityData.get(DATA_RUNE_IDS);
        if (ids.isEmpty()) return;

        runeChain.clear();
        String[] idArray = ids.split(",");
        RuneRegistry registry = RuneRegistry.getInstance();

        for (String id : idArray) {
            Rune rune = registry.getRuneById(id.trim());
            if (rune != null) {
                runeChain.add(rune);
            }
        }

        runesInitialized = !runeChain.isEmpty();
    }

    @Override
    public void tick() {
        super.tick();

        // 客户端需要从网络数据重建符文链
        if (level().isClientSide && !runesInitialized) {
            rebuildRuneChain();
        }

        // TODO: 添加飞行粒子效果
        // if (level().isClientSide) {
        //     spawnTrailParticles();
        // }

        // 生存时间限制（10秒后消失）
        if (this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (level().isClientSide) {
            return;
        }

        // 确保符文链已初始化
        if (!runesInitialized) {
            rebuildRuneChain();
        }

        if (runeChain.isEmpty()) {
            this.discard();
            return;
        }

        // 在命中位置执行符文链
        if (getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            // 创建执行上下文
            RuneContext context = new RuneContext(player, level());
            context.setPosition(this.position());
            context.setDirection(this.getDeltaMovement().normalize());

            // 执行符文链（跳过触发符文，从形状符文开始）
            List<Rune> effectChain = new ArrayList<>();
            for (int i = 1; i < runeChain.size(); i++) { // 跳过第一个触发符文
                effectChain.add(runeChain.get(i));
            }

            if (!effectChain.isEmpty()) {
                // 手动执行符文链（不通过RuneChainExecutor，因为已经跳过触发符文）
                for (Rune rune : effectChain) {
                    rune.execute(context);
                }
            }
        }

        // 显示命中效果
        spawnHitParticles();

        // 移除实体
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        onHit(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        onHit(result);
    }

    /**
     * 生成飞行轨迹粒子
     */
    private void spawnTrailParticles() {
        // TODO: 添加粒子效果
        // level().addParticle(ParticleTypes.ENCHANT,
        //     this.getX(), this.getY(), this.getZ(),
        //     0, 0, 0);
    }

    /**
     * 生成命中粒子效果
     */
    private void spawnHitParticles() {
        // TODO: 添加命中粒子效果
        // for (int i = 0; i < 10; i++) {
        //     level().addParticle(ParticleTypes.EXPLOSION,
        //         this.getX(), this.getY(), this.getZ(),
        //         random.nextGaussian() * 0.2, random.nextGaussian() * 0.2, random.nextGaussian() * 0.2);
        // }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        // 保存符文ID列表
        ListTag runeList = new ListTag();
        for (Rune rune : runeChain) {
            runeList.add(StringTag.valueOf(rune.getId()));
        }
        tag.put("RuneChain", runeList);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        // 读取符文ID列表
        if (tag.contains("RuneChain")) {
            ListTag runeList = tag.getList("RuneChain", 8); // 8 = STRING
            runeChain.clear();
            RuneRegistry registry = RuneRegistry.getInstance();

            for (int i = 0; i < runeList.size(); i++) {
                String id = runeList.getString(i);
                Rune rune = registry.getRuneById(id);
                if (rune != null) {
                    runeChain.add(rune);
                }
            }
            runesInitialized = !runeChain.isEmpty();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

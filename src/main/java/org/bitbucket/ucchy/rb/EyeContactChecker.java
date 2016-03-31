/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.rb;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * プレイヤー同士が見つめあっているかどうかをチェックするタスク
 * @author ucchy
 */
public class EyeContactChecker extends BukkitRunnable {

    private static final double DISTANCE = 1.5;
    private static final double RANGE_SQRT = DISTANCE * DISTANCE;

    private RiajuuBakuhatsu parent;
    private Player player;
    private boolean isCancelled;

    /**
     * コンストラクタ
     * @param player チェック対象プレイヤー
     * @param parent 呼び出し元プラグイン
     */
    public EyeContactChecker(Player player, RiajuuBakuhatsu parent) {
        this.player = player;
        this.parent = parent;
        this.isCancelled = false;
    }

    /**
     * タイマー間隔ごとに呼び出されるメソッド
     */
    @Override
    public void run() {

        // 対象プレイヤーが既にスニーキングしていないなら、タスクをキャンセルする
        if ( !player.isSneaking() ) {
            cancel();
            isCancelled = true;
        }

        // 同じワールド内のプレイヤーと見つめあっていないかどうかをチェックする
        for ( Player target : player.getWorld().getPlayers() ) {

            if ( player.equals(target) ) continue;

            if ( isEyeContactingPlayers(player, target) ) {

                // スニークを強制解除する（爆発の重複発生を防ぐため）
                player.setSneaking(false);
                target.setSneaking(false);

                // メッセージを流す
                String message = ChatColor.RED + "リア充爆発しろ！";
                player.sendMessage(message);
                target.sendMessage(message);

                // 爆破する
                Location center = getCenterLocation(player.getLocation(), target.getLocation());

                center.getWorld().createExplosion(
                        center.getX(), center.getY(), center.getZ(),
                        (float)parent.explosionPower, false, parent.destructTerrain);

                // ふっとばす
                Vector vec1 = player.getLocation().subtract(center).add(0, 0.5, 0).toVector();
                vec1.normalize().multiply(parent.nockbackPower);
                player.setVelocity(vec1);
                Vector vec2 = target.getLocation().subtract(center).add(0, 0.5, 0).toVector();
                vec2.normalize().multiply(parent.nockbackPower);
                target.setVelocity(vec2);
            }
        }
    }

    /**
     * プレイヤー達が見つめあっているかどうかを判定します。
     * @param player1 プレイヤー1
     * @param player2 プレイヤー2
     * @return 見つめあっているかどうか
     */
    private static boolean isEyeContactingPlayers(Player player1, Player player2) {

        // プレイヤー1が見ている辺りにプレイヤー2がいるかどうかを判定する
        Location loc = player1.getLocation().clone();
        loc.add(player1.getLocation().getDirection().normalize().multiply(DISTANCE));
        if ( loc.distanceSquared(player2.getLocation()) > RANGE_SQRT ) return false;

        // プレイヤー2が見ている辺りにプレイヤー1がいるかどうかを判定する
        loc = player2.getLocation().clone();
        loc.add(player2.getLocation().getDirection().normalize().multiply(DISTANCE));
        if ( loc.distanceSquared(player1.getLocation()) > RANGE_SQRT ) return false;

        // 両プレイヤーともスニーキングしているなら、見つめあっていると判定する。
        return player1.isSneaking() && player2.isSneaking();
    }

    /**
     * 2つのLocationの中間地点を返す
     * @param loc1 地点1
     * @param loc2 地点2
     * @return 中間地点
     */
    private static Location getCenterLocation(Location loc1, Location loc2) {
        return new Location(loc1.getWorld(),
                (loc1.getX() + loc2.getX()) / 2.0,
                (loc1.getY() + loc2.getY()) / 2.0,
                (loc1.getZ() + loc2.getZ()) / 2.0 );
    }

    /**
     * このタスクは既にキャンセルされたものかどうかを返す
     * @return キャンセルされているかどうか
     */
    protected boolean isCancelled() {
        return isCancelled;
    }
}

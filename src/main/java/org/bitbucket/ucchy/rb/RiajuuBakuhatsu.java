/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.rb;

import java.util.Calendar;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * リア充爆発プラグイン
 * @author ucchy
 */
public class RiajuuBakuhatsu extends JavaPlugin implements Listener {

    /** 目が合っているかどうかのチェック間隔（ticks） */
    private static final int CHECK_PERIOD_TICKS = 5;

    /**
     * 各プレイヤーがスニークしている間、目が合っているプレイヤーがいないか検索するためのタスク。
     * このタスクはプレイヤーごとに作成するものであり、スニークごとに作成するわけではないので、
     * 消費メモリは大した量ではないはずである。
     */
    private HashMap<String, EyeContactChecker> checkers;

    // 以下、コンフィグ項目
    private boolean aprilfoolTimer;
    protected boolean destructTerrain;
    protected double explosionPower;
    protected double nockbackPower;

    /**
     * コンストラクタ
     */
    public RiajuuBakuhatsu() {
        checkers = new HashMap<String, EyeContactChecker>();
    }

    /**
     * プラグインが有効化されたときに呼び出されるメソッドです。
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // リスナー登録
        Bukkit.getPluginManager().registerEvents(this, this);

        // コンフィグのロード
        saveDefaultConfig();
        reloadConfig();

        this.aprilfoolTimer = getConfig().getBoolean("aprilfoolTimer", true);
        this.destructTerrain = getConfig().getBoolean("destructTerrain", false);
        this.explosionPower = getConfig().getDouble("explosionPower", 2.5);
        this.nockbackPower = getConfig().getDouble("nockbackPower", 2.0);
    }

    /**
     * プレイヤーが、スニークしたり解除したりしたときに呼び出されるメソッド。
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {

        // スニークを解除したイベントなら無視する
        if ( !event.isSneaking() ) return;

        // エイプリルフールタイマーが有効なら、4月1日じゃないときは動作しない
        if ( aprilfoolTimer ) {
            Calendar cal = Calendar.getInstance();
            if ( cal.get(Calendar.MONTH) != Calendar.APRIL ||
                    cal.get(Calendar.DAY_OF_MONTH) != 1 ) {
                return;
            }
        }

        // チェックタスクが無いなら、新規に登録する。
        String name = event.getPlayer().getName();
        if ( !checkers.containsKey(name) || checkers.get(name).isCancelled() ) {
            EyeContactChecker checker = new EyeContactChecker(event.getPlayer(), this);
            checker.runTaskTimer(this, CHECK_PERIOD_TICKS, CHECK_PERIOD_TICKS);
            checkers.put(name, checker);
        }
    }

    /**
     * 生物がスポーンした時に呼び出される
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {

        // bleeding以外は無視する
        if ( event.getSpawnReason() != SpawnReason.BREEDING ) {
            return;
        }

        // エイプリルフールタイマーが有効なら、4月1日じゃないときは動作しない
        if ( aprilfoolTimer ) {
            Calendar cal = Calendar.getInstance();
            if ( cal.get(Calendar.MONTH) != Calendar.APRIL ||
                    cal.get(Calendar.DAY_OF_MONTH) != 1 ) {
                return;
            }
        }

        // イベントをキャンセルしてから、爆破する。
        event.setCancelled(true);
        final Location loc = event.getLocation();
        new BukkitRunnable() {
            public void run() {
                loc.getWorld().createExplosion(
                        loc.getX(), loc.getY(), loc.getZ(),
                        (float)explosionPower, false, destructTerrain);
            }
        }.runTaskLater(this, 2);
    }
}

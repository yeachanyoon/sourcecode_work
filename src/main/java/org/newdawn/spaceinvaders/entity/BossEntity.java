package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

/** 보스 엔티티 (보스 처치까지 클리어) */
public class BossEntity extends Entity {
    private final Game game;
    private final Sprite sprite;

    private int maxHP = 800;
    private int hp = maxHP;

    private double speed = 120;   // 좌우 이동 속도(px/s)
    private int leftBound = 40;   // 이동 경계
    private int rightBound = 760;

    private long lastShot = 0;
    private long shotInterval = 900; // 탄막 간격(ms)

    public BossEntity(Game game, int x, int y) {
        super("sprites/Boss.png", x, y);
        this.game = game;
        this.sprite = SpriteStore.get().getSprite("sprites/Boss.png");
        setHorizontalMovement(speed);
        setVerticalMovement(0);
    }

    /** 이동 + 주기적 탄막 */
    public void move(long delta) {
        super.move(delta);

        if (getX() < leftBound) {
            setX(leftBound);
            setHorizontalMovement(Math.abs(getHorizontalMovement()));
        } else if (getX() + getWidth() > rightBound) {
            setX(rightBound - getWidth());
            setHorizontalMovement(-Math.abs(getHorizontalMovement()));
        }

        long now = System.currentTimeMillis();
        if (now - lastShot >= shotInterval) {
            lastShot = now;
            firePattern();
        }
    }

    /** 3갈래 탄막(중앙/좌/우) */
    private void firePattern() {
        int cx = getX() + getWidth() / 2;
        int cy = getY() + getHeight() - 10;

        AlienShotEntity s0 = new AlienShotEntity(game, "sprites/shot.gif", cx, cy);
        s0.setHorizontalMovement(0);
        game.addEntity(s0);

        AlienShotEntity sL = new AlienShotEntity(game, "sprites/shot.gif", cx - 8, cy);
        sL.setHorizontalMovement(-80);
        game.addEntity(sL);

        AlienShotEntity sR = new AlienShotEntity(game, "sprites/shot.gif", cx + 8, cy);
        sR.setHorizontalMovement(+80);
        game.addEntity(sR);
    }

    public void takeDamage(int amount) {
        if (amount <= 0) return;
        hp -= amount;
        if (hp <= 0) {
            hp = 0;
            game.onBossDefeated(this);
            game.removeEntity(this);
        }
    }

    public boolean isDead() { return hp <= 0; }
    public int  getHP()     { return hp; }
    public int  getMaxHP()  { return maxHP; }

    /** 스프라이트 렌더 */
    public void draw(Graphics2D g) {
        if (sprite != null) sprite.draw(g, getX(), getY());
    }

    /** 필요 시 호출되는 게임 로직 훅(비워둠) */
    public void doLogic() {
        // 보스 개별 추가 로직이 필요하면 여기에
    }

    /** 충돌 처리 */
    public void collidedWith(Entity other) {
        if (other instanceof ShotEntity) {
            takeDamage(10);
            game.removeEntity(other);
            return;
        }
        if (other instanceof ShipEntity) {
            game.playerHit();
        }
    }

    /* ===== 레벨별 조절을 위한 보스 파라미터 세터/게터 ===== */

    /** 현재 탄막 간격(ms) 반환 */
    public long getShotInterval() { return shotInterval; }

    /**
     * 탄막 간격(ms) 설정. 너무 작은 값은 과도하니 하한선을 둡니다.
     * @param ms 간격(밀리초)
     */
    public void setShotInterval(long ms) {
        this.shotInterval = Math.max(200, ms);
    }

    /** 좌우 이동 속도(px/s) 설정(옵션) */
    public void setSpeed(double pxPerSec) {
        this.speed = pxPerSec;
        setHorizontalMovement(Math.signum(getHorizontalMovement()) == 0 ? speed : Math.copySign(speed, getHorizontalMovement()));
    }
}

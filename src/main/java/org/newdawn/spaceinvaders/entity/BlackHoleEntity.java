package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;
import org.newdawn.spaceinvaders.SystemTimer;

/**
 * 블랙홀 엔티티
 * - 감속 적용은 Game에서 "플레이어 이동속도 계산" 시만 이뤄진다.
 * - 여기서는 위치/수명/렌더만 관리한다.
 */
public class BlackHoleEntity extends Entity {
    private final Game game;

    private final double radius;       // 감속 반경(시각적/논리적)
    private final double strength;     // (지금은 Game에서 0.5 고정 스케일 사용, 보관만)
    private final long spawnTime;      // 생성 시각
    private final long lifetimeMs;     // 지속 시간(ms)
    private final Sprite sprite;       // 스프라이트 (gif)

    public BlackHoleEntity(Game game, int x, int y,
                           double radius, double strength, long lifetimeMs) {
        super("sprites/blackhole.gif", x, y);
        this.game = game;
        this.radius = radius;
        this.strength = strength;
        this.lifetimeMs = lifetimeMs;
        this.spawnTime = SystemTimer.getTime();

        this.sprite = SpriteStore.get().getSprite("sprites/blackhole.gif");

        setHorizontalMovement(0);
        setVerticalMovement(0);
    }

    @Override
    public void move(long delta) {
        long now = SystemTimer.getTime();
        if (now - spawnTime >= lifetimeMs) {
            game.removeEntity(this);
            return;
        }
        // 감속은 Game이 플레이어 속도 계산할 때만 적용
    }

    public double getRadius()   { return radius; }
    public double getStrength() { return strength; }

    public int getWidth()  { return (sprite != null) ? sprite.getWidth()  : 64; }
    public int getHeight() { return (sprite != null) ? sprite.getHeight() : 64; }

    @Override
    public void draw(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        if (sprite != null && sprite.getWidth() > 0) {
            sprite.draw(g, (int) getX(), (int) getY());
        } else {
            int r = (int) radius;
            g.setColor(new Color(80, 80, 200, 150));
            g.fillOval((int)(getX() + getWidth()/2.0 - r), (int)(getY() + getHeight()/2.0 - r), r * 2, r * 2);
        }
    }

    @Override public void doLogic() { /* 없음 */ }
    @Override public void collidedWith(Entity other) { /* 없음 */ }
}

package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics;
import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.SystemTimer;

public class BlackHoleEntity extends Entity {
    private final Game game;

    //초 간격으로 생성
    private long lifetime = 5000;
    private long bornAt;

    public BlackHoleEntity(Game game, int x, int y) {
        super("sprites/blackhole.gif", x, y);
        this.game = game;
        this.bornAt = SystemTimer.getTime(); //생성 시각 기록
    }

    @Override
    public void move(long delta) {
        // ★ 수명 끝났으면 제거 + 새 블랙홀 즉시 스폰
        if (SystemTimer.getTime() - bornAt >= lifetime) {
            game.removeEntity(this);
            game.flagBlackHoleRespawn();  //리스폰
        }
    }

    @Override
    public void draw(Graphics g) {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
        java.awt.Composite old = g2.getComposite();
        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.6f));
        super.draw(g2);
        g2.setComposite(old);
    }

    @Override
    public void collidedWith(Entity other) { /* 감쇠는 Game에서 거리로 처리 */ }
}


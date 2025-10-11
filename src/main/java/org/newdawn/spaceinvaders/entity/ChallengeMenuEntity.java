package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import org.newdawn.spaceinvaders.Game;

/** 도전과제 화면 전용 엔티티 (그리기만 수행) */
public class ChallengeMenuEntity extends Entity {
    private final Game game;

    public ChallengeMenuEntity(Game game) {
        super("sprites/ship.gif", 0, 0);
        this.game = game;
    }

    public void move(long delta) {
        // no-op
    }

    public void draw(Graphics g) {
        // 부모 시그니처와 동일하게 오버라이드
        game.renderChallengeScreen((Graphics2D) g);
    }

    public void doLogic() {
        // no-op
    }

    public boolean collidesWith(Entity other) {
        return false;
    }

    public void collidedWith(Entity other) {
        // no-op
    }
}


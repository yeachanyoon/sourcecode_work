package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import org.newdawn.spaceinvaders.Game;

/** 메인 메뉴 전용 엔티티 (그리기만 수행) */
public class MainMenuEntity extends Entity {
    private final Game game;

    public MainMenuEntity(Game game) {
        // 부모 생성자에 더미 스프라이트 전달 (부모 draw는 사용하지 않음)
        super("sprites/ship.gif", 0, 0);
        this.game = game;
    }

    public void move(long delta) {
        // no-op
    }

    public void draw(Graphics g) {
        // 부모 시그니처와 동일하게 오버라이드! 내부에서만 2D로 캐스팅
        game.renderMainMenu((Graphics2D) g);
    }

    public void doLogic() {
        // no-op
    }

    public boolean collidesWith(Entity other) {
        return false; // 메뉴는 충돌하지 않음
    }

    public void collidedWith(Entity other) {
        // no-op (충돌 처리 없음)
    }
}

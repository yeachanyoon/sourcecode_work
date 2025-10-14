package org.newdawn.spaceinvaders.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import org.newdawn.spaceinvaders.Game;

/** 게임 중 우상단 Score HUD 전용 엔티티(충돌/이동 없음) */
public class PlayScoreEntity extends Entity {
    private final Game game;

    // UI 전용이지만 상위 생성자 시그니처를 맞추기 위해 더미 스프라이트 사용
    public PlayScoreEntity(Game game) {
        super("sprites/ship.gif", -10000, -10000); // 화면 밖
        this.game = game;
        setHorizontalMovement(0);
        setVerticalMovement(0);
    }

    @Override
    public void move(long delta) {
        // no-op (HUD)
    }

    // ★ 핵심: Graphics2D가 아니라 Graphics를 사용해야 상위 Entity.draw(...)와 시그니처 일치
    @Override
    public void draw(Graphics g) {
        if (game == null || game.isWaitingForMenu()) return; // 메뉴 상태면 숨김
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        String scoreStr = "Score: " + game.getScore();
        g.drawString(scoreStr, Game.VIRTUAL_WIDTH - 150, 20);
    }

    @Override
    public void doLogic() {
        // no-op
    }

    @Override
    public void collidedWith(Entity other) {
        // no-op (HUD)
    }
}

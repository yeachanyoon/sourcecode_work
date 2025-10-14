package org.newdawn.spaceinvaders.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.Game;

public class ChallengeMenuEntity extends Entity {
    private final Game game;
    private boolean visible = false;

    public ChallengeMenuEntity(Game game) {
        super("sprites/ship.gif", 0, 0);
        this.game = game;
    }

    public void move(long delta) { }
    public void doLogic() { }
    public void collidedWith(Entity other) { }

    // ✅ 부모와 동일 시그니처로 수정: Graphics
    public void draw(Graphics g0) {
        if (!visible) return;
        Graphics2D g = (Graphics2D) g0;

        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, 800, 600);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        String title = "도전 과제";
        FontMetrics fm = g.getFontMetrics();
        int x = (800 - fm.stringWidth(title)) / 2;
        g.drawString(title, x, 120);

        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        int y = 180;
        g.drawString("• 적 10마리 처치",       220, y); y += 30;
        g.drawString("• 100발 이하로 클리어", 220, y); y += 30;
        g.drawString("• 1분 안에 클리어",     220, y);
    }

    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isVisible() { return visible; }
}

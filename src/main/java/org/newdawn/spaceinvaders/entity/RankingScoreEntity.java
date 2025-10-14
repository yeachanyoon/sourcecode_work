package org.newdawn.spaceinvaders.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.List;

import org.newdawn.spaceinvaders.Game;

/** 도전과제 화면 하단에 Top Scores 표기하는 엔티티 */
public class RankingScoreEntity extends Entity {
    private final Game game;

    public RankingScoreEntity(Game game) {
        super("sprites/ship.gif", -10000, -10000); // 화면 밖
        this.game = game;
        setHorizontalMovement(0);
        setVerticalMovement(0);
    }

    @Override
    public void move(long delta) { /* no-op */ }

    // ★ Graphics 사용
    @Override
    public void draw(Graphics g) {
        if (!game.isShowingChallenge()) return; // 도전과제 화면일 때만 그림

        // 제목
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString("Top Scores", 220, 300);

        // 목록
        List<Game.RankRow> top = game.getTopScores(5);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        FontMetrics fm = g.getFontMetrics();
        int yy = 330;
        for (int i = 0; i < top.size(); i++) {
            Game.RankRow r = top.get(i);
            String line = String.format("%2d) %-12s  %7d", i + 1, r.name, r.score);
            g.drawString(line, 230, yy);
            yy += fm.getHeight() + 4;
        }
    }

    @Override
    public void doLogic() { /* no-op */ }

    @Override
    public void collidedWith(Entity other) { /* no-op */ }
}

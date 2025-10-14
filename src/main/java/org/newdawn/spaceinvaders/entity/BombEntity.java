package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.SystemTimer;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

/**
 * BombEntity: DROP(떨어지는아이템) / PROJECTILE(발사체) / EXPLODING(폭발)
 * - DROP: 에일리언 죽은 자리에서 생성되어 천천히 아래로 떨어짐. Ship이 닿으면 보유+1 (최대는 Game에서 관리)
 * - PROJECTILE: B키로 발사. 화면 밖/적 충돌 시 EXPLODING으로 전환
 * - EXPLODING: 네가 넣어둔 explosion 스프라이트로 폭발을 그린다. (논리 효과는 Game.activateBombAt)
 */
public class BombEntity extends Entity {

    public enum Mode { DROP, PROJECTILE, EXPLODING }

    private final Game game;
    private Mode mode;

    // 움직임 속도
    private final double dropSpeedY = 120;   // 천천히 낙하
    private final double shotSpeedY = -450;  // 위로 발사

    // 폭발 이펙트
    private final long explosionLifeMs = 550; // 폭발 표시 시간(애니 gif라면 대충 이 정도면 자연스러움)
    private long explodeStart = -1;

    // 폭발 스프라이트 (png 우선, 없으면 gif 시도)
    private Sprite explosionSprite;

    public BombEntity(Game game, int x, int y) {
        super("sprites/bomb.png", x, y);
        this.game = game;
        this.explosionSprite = loadExplosionSprite();
        setMode(Mode.DROP); // 기본은 DROP(떨어지는 아이템)
    }

    /** explosion 스프라이트 로드: png 우선, 실패 시 gif */
    private Sprite loadExplosionSprite() {
        SpriteStore store = SpriteStore.get();
        Sprite s = null;
        try { s = store.getSprite("sprites/explosion.png"); } catch (RuntimeException ignore) {}
        if (s == null) {
            try { s = store.getSprite("sprites/explosion.gif"); } catch (RuntimeException ignore) {}
        }
        return s; // 둘 다 없으면 null -> 아래에서 대비 렌더 안함(그냥 안 보임)
    }

    public final void setMode(Mode m) {
        this.mode = m;
        switch (m) {
            case DROP:
                setHorizontalMovement(0);
                setVerticalMovement(dropSpeedY);
                break;
            case PROJECTILE:
                setHorizontalMovement(0);
                setVerticalMovement(shotSpeedY);
                break;
            case EXPLODING:
                setHorizontalMovement(0);
                setVerticalMovement(0);
                explodeStart = SystemTimer.getTime();
                // 폭발 논리는 Game.activateBombAt에서 수행
                int cx = (int) (getX() + getWidth()  / 2.0);
                int cy = (int) (getY() + getHeight() / 2.0);
                game.activateBombAt(cx, cy);
                break;
        }
    }

    @Override
    public void move(long delta) {
        switch (mode) {
            case DROP:
                super.move(delta);
                if (getY() > Game.VIRTUAL_HEIGHT) {
                    game.removeEntity(this);
                }
                break;

            case PROJECTILE:
                super.move(delta);
                if (getY() + getHeight() < 0) {
                    setMode(Mode.EXPLODING);
                }
                break;

            case EXPLODING:
                if (SystemTimer.getTime() - explodeStart >= explosionLifeMs) {
                    game.removeEntity(this);
                }
                break;
        }
    }

    @Override public void doLogic() { /* 없음 */ }

    @Override
    public void collidedWith(Entity other) {
        if (mode == Mode.DROP) {
            if (other instanceof ShipEntity) {
                if (game.collectBomb()) {
                    game.removeEntity(this);
                }
            }
            return;
        }

        if (mode == Mode.PROJECTILE) {
            if (other instanceof AlienEntity || other instanceof AlienShotEntity || other instanceof AsteroidEntity) {
                setMode(Mode.EXPLODING);
            }
            return;
        }

        // EXPLODING 중에는 충돌 무시
    }

    @Override
    public void draw(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;

        if (mode == Mode.EXPLODING) {
            // 폭발 스프라이트가 있으면 중앙에 정렬해서 그린다.
            if (explosionSprite != null) {
                int w = explosionSprite.getWidth();
                int h = explosionSprite.getHeight();
                int cx = (int) (getX() + getWidth()  / 2.0);
                int cy = (int) (getY() + getHeight() / 2.0);
                explosionSprite.draw(g, cx - w / 2, cy - h / 2);
            }
            // 스프라이트가 없으면 아무것도 그리지 않음(논리 효과는 이미 적용됨)
            return;
        }

        // DROP / PROJECTILE: 스프라이트로 표시
        if (sprite != null && sprite.getWidth() > 0) {
            sprite.draw(g, (int) getX(), (int) getY());
        } else {
            // 폭탄 본체가 없을 때는 아무 것도 그리지 않음(혹은 간단한 원으로 대체 가능)
        }
    }
}

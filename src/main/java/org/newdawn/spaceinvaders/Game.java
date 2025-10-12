package org.newdawn.spaceinvaders;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.newdawn.spaceinvaders.entity.*;

/**
 * The main hook of our game. This class with both act as a manager
 * for the display and central mediator for the game logic. 
 * 
 * Display management will consist of a loop that cycles round all
 * entities in the game asking them to move and then drawing them
 * in the appropriate place. With the help of an inner class it
 * will also allow the player to control the main ship.
 * 
 * As a mediator it will be informed when entities within our game
 * detect events (e.g. alient killed, played died) and will take
 * appropriate game actions.
 * 
 * @author Kevin Glass
 */
public class Game extends Canvas
{
	/** The stragey that allows us to use accelerate page flipping */
	private BufferStrategy strategy;
	/** True if the game is currently "running", i.e. the game loop is looping */
	private boolean gameRunning = true;
	/** The list of all the entities that exist in our game */
	private ArrayList entities = new ArrayList();
	/** The list of entities that need to be removed from the game this loop */
	private ArrayList removeList = new ArrayList();
	/** The entity representing the player */
	private Entity ship;
	/** The speed at which the player's ship should move (pixels/sec) */
	private double moveSpeed = 300;
	/** The time at which last fired a shot */
	private long lastFire = 0;
	/** The interval between our players shot (ms) */
	private long firingInterval = 500;
	/** The number of aliens left on the screen */
	private int alienCount;
	
	/** The message to display which waiting for a key press */
	private String message = "";
	/** True if we're holding up game play until a key has been pressed */
	private boolean waitingForKeyPress = true;
	/** True if the left cursor key is currently pressed */
	private boolean leftPressed = false;
	/** True if the right cursor key is currently pressed */
	private boolean rightPressed = false;
	/** True if we are firing */
	private boolean firePressed = false;
	/** True if game logic needs to be applied this loop, normally as a result of a game event */
	private boolean logicRequiredThisLoop = false;
	/** The last time at which we recorded the frame rate */
	private long lastFpsTime;
	/** The current number of frames recorded */
	private int fps;
	/** The normal title of the game window */
	private String windowTitle = "Space Invaders 102";
	/** The game window that we'll update with the frame count */
	private JFrame container;


    public static final int VIRTUAL_WIDTH = 800;
    public static final int VIRTUAL_HEIGHT = 600;

    private int screenW, screenH;
    private double scale;
    private int offsetX, offsetY;

    private int  laserDps  = 500;
    private int  beamWidth  = 8;    // 픽

    private boolean bossSpawned = false;
    private BossEntity boss;

    private long lastAlienFire = 0;
    private long alienShootingInterval = 500; //에일리언이 총알 쏘는 시간 간격, 0.5초로 설정함
    private java.util.Random rng = new java.util.Random();


    private int lives = 3;   //플레이어 현재 목숨
    private long invincibleMs = 2000;   // 피격 후 무적 유지 시간
    private long invincibleUntil = 0; //무적이 끝나는 시각

    private int maxLives = 3; //최대 체력
    private int heartSize = 24;
    private int heartSpacing = 6;   // 하트 사이 간격
    private int heartMarginX = -100, heartMarginY = 10; // 좌측 상단 여백

    private org.newdawn.spaceinvaders.Sprite heartFull;
    private org.newdawn.spaceinvaders.Sprite heartEmpty;

    // 레이저 자동 발사 종료시각
    private long laserAutoUntil = 0;

    private long nextItemSpawn = 0;
    private long itemSpawnInterval = 10000;

    private int bombRadius     = 120; // 폭발 반경
    private int bombBaseDamage = 200; // 보스에 들어갈 기본 피해


    private boolean blackHoleRespawnPending = false;
    private long nextBlackholeSpawn = 0;       // 0이면 카운트 중지
    private long blackholeSpawnInterval = 5000; //5초 간격으로 블랙홀 생성
    private int  blackholeRadius = 90;
    private double blackholeMultiplier = 0.3;

    private long nextAsteroidSpawn = 0; // 0이면 현재값부터 카운트
    private long asteroidInterval = 3000; // 3초에 한 번 소행성 떨어짐


    /** 현재 자동 레이저가 활성인지 여부 */
    private boolean isLaserAutoActive() { return SystemTimer.getTime() < laserAutoUntil; }

    /**
	 * Construct our game and set it running.
	 */
	public Game() {
        container = new JFrame("Space Invaders 102");

        // 전체화면 진입 (Exclusive Fullscreen)
        container.setUndecorated(true);
        container.setIgnoreRepaint(true);

        JPanel panel = (JPanel) container.getContentPane();
        panel.setLayout(null);

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        gd.setFullScreenWindow(container);

        // 실제 화면 크기 얻기
        screenW = container.getGraphicsConfiguration().getBounds().width;
        screenH = container.getGraphicsConfiguration().getBounds().height;

        // 캔버스를 화면 크기로 붙이기
        setBounds(0, 0, screenW, screenH);
        panel.add(this);
        container.validate();

        // 논리→실제 스케일/오프셋 계산
        setFullScreen();

        // AWT 자동 리페인트 차단(기존과 동일)
        setIgnoreRepaint(true);

		
		// add a listener to respond to the user closing the window. If they
		// do we'd like to exit the game
		container.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		
		// add a key input system (defined below) to our canvas
		// so we can respond to key pressed
		addKeyListener(new KeyInputHandler());
		
		// request the focus so key events come to us
		requestFocus();

		// create the buffering strategy which will allow AWT
		// to manage our accelerated graphics
		createBufferStrategy(2);
		strategy = getBufferStrategy();

        setFullScreen();

		// initialise the entities in our game so there's something
		// to see at startup
		initEntities();



        heartFull  = org.newdawn.spaceinvaders.SpriteStore.get().getSprite("sprites/full_heart.png");
        heartEmpty = org.newdawn.spaceinvaders.SpriteStore.get().getSprite("sprites/empty_heart.png");

    }
    private void setFullScreen() {
        double sx = (double) screenW / VIRTUAL_WIDTH;
        double sy = (double) screenH / VIRTUAL_HEIGHT;
        scale = Math.min(sx, sy);

        int drawW = (int) Math.round(VIRTUAL_WIDTH * scale);
        int drawH = (int) Math.round(VIRTUAL_HEIGHT * scale);
        offsetX = (screenW - drawW) / 2;
        offsetY = (screenH - drawH) / 2;
    }
	/**
	 * Start a fresh game, this should clear out any old data and
	 * create a new set.
	 */
	private void startGame() {
        lives = maxLives;
        invincibleUntil = 0;


        // clear out any existing entities and intialise a new set
		entities.clear();
		initEntities();
		
		// blank out any keyboard settings we might currently have
		leftPressed = false;
		rightPressed = false;
		firePressed = false;
	}
	
	/**
	 * Initialise the starting state of the entities (ship and aliens). Each
	 * entitiy will be added to the overall list of entities in the game.
	 */
	private void initEntities() {
		// create the player ship and place it roughly in the center of the screen
		ship = new ShipEntity(this,"sprites/ship.gif",370,550);
		entities.add(ship);
		
		// create a block of aliens (5 rows, by 12 aliens, spaced evenly)
		alienCount = 0;
		for (int row=0;row<5;row++) {
			for (int x=0;x<12;x++) {
				Entity alien = new AlienEntity(this,100+(x*50),(50)+row*30);
				entities.add(alien);
				alienCount++;
			}
		}


	}
	
	/**
	 * Notification from a game entity that the logic of the game
	 * should be run at the next opportunity (normally as a result of some
	 * game event)
	 */
	public void updateLogic() {
		logicRequiredThisLoop = true;
	}
	
	/**
	 * Remove an entity from the game. The entity removed will
	 * no longer move or be drawn.
	 *
	 * @param entity The entity that should be removed
	 */
	public void removeEntity(Entity entity) {
		removeList.add(entity);
	}


	/**
	 * Notification that the player has died.
	 */
	public void notifyDeath() {
		message = "Oh no! They got you, try again?";
		waitingForKeyPress = true;
	}
	
	/**
	 * Notification that the player has won since all the aliens
	 * are dead.
	 */
	public void notifyWin() {
		message = "Well done! You Win!";
		waitingForKeyPress = true;
	}
	
	/**
	 * Notification that an alien has been killed
	 */
	public void notifyAlienKilled() {
		// reduce the alient count, if there are none left, the player has won!
		alienCount--;



        //필드에 private boolean bossSpawned = false;
		if (alienCount == 0) {
            if (!bossSpawned) {
                spawnBoss();
            }
            return;
		}



		// if there are still some aliens left then they all need to get faster, so
		// speed up all the existing aliens
		for (int i=0;i<entities.size();i++) {
			Entity entity = (Entity) entities.get(i);
			
			if (entity instanceof AlienEntity) {
				// speed up by 2%
				entity.setHorizontalMovement(entity.getHorizontalMovement() * 1.02);
			}
		}
	}

    //플레이어가 피격 당했을 때 호출. 총알, 소행성 등
    public void playerHit() {
        // 무적 상태면 무시
        if (isPlayerInvincible()) return;

        lives--;
        if (lives <= 0) {
            notifyDeath();
            return;
        }

        //언제까지 무적인지
        invincibleUntil = SystemTimer.getTime() + invincibleMs;

        // 배를 시작 위치로 리스폰
        if (ship != null) {
            ship.setPosition(370, 550);
            ship.setHorizontalMovement(0);
            ship.setVerticalMovement(0);
        }

        for (int i = 0; i < entities.size(); i++) {
            Entity e = (Entity) entities.get(i);
            if (e instanceof AlienShotEntity) {
                removeEntity(e);
            }
        }
    }


    public void notifyBossKilled() {
        // 보스는 한 마리만 가정
        message = "보스를 죽이다니..ㄷㄷ 축하합니다!";
        waitingForKeyPress = true;
    }

    /** 레이저 자동 발사 파워업 활성화 */
    public void activateLaser(long durationMs) {
        laserAutoUntil = Math.max(laserAutoUntil, SystemTimer.getTime()) + durationMs;
    }
	
	/**
	 * Attempt to fire a shot from the player. Its called "try"
	 * since we must first check that the player can fire at this 
	 * point, i.e. has he/she waited long enough between shots
	 */
	public void tryToFire() {
		// check that we have waiting long enough to fire
		if (System.currentTimeMillis() - lastFire < firingInterval) {
			return;
		}
		
		// if we waited long enough, create the shot entity, and record the time.
		lastFire = System.currentTimeMillis();
		ShotEntity shot = new ShotEntity(this,"sprites/shot.gif",ship.getX()+10,ship.getY()-30);
		entities.add(shot);
	}
	
	/**
	 * The main game loop. This loop is running during all game
	 * play as is responsible for the following activities:
	 * <p>
	 * - Working out the speed of the game loop to update moves
	 * - Moving the game entities
	 * - Drawing the screen contents (entities, text)
	 * - Updating game events
	 * - Checking Input
	 * <p>
	 */
	public void gameLoop() {
		long lastLoopTime = SystemTimer.getTime();
		
		// keep looping round til the game ends
		while (gameRunning) {
			// work out how long its been since the last update, this
			// will be used to calculate how far the entities should
			// move this loop
			long delta = SystemTimer.getTime() - lastLoopTime;
			lastLoopTime = SystemTimer.getTime();

			// update the frame counter
			lastFpsTime += delta;
			fps++;
			
			// update our FPS counter if a second has passed since
			// we last recorded
			if (lastFpsTime >= 1000) {
				container.setTitle(windowTitle+" (FPS: "+fps+")");
				lastFpsTime = 0;
				fps = 0;
			}

            Graphics2D g = (Graphics2D) strategy.getDrawGraphics();

            //실제 화면 전체를 먼저 검은색으로
            g.setColor(Color.black);
            g.fillRect(0, 0, screenW, screenH);


            // 2) 논리 해상도(800x600)로 변환
            g.translate(offsetX, offsetY);
            g.scale(scale, scale);


            g.setColor(Color.black);
            g.fillRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
			
			// cycle round asking each entity to move itself
			if (!waitingForKeyPress) {
				for (int i=0;i<entities.size();i++) {
					Entity entity = (Entity) entities.get(i);
					
					entity.move(delta);
				}
			}

            if (!waitingForKeyPress) {
                long now = SystemTimer.getTime();
                if (nextAsteroidSpawn == 0) {
                    nextAsteroidSpawn = now + asteroidInterval; // 첫 예약
                }
                if (now >= nextAsteroidSpawn) {
                    spawnAsteroid();
                    nextAsteroidSpawn = now + asteroidInterval; // 다음 예약
                }
            }

            if (!waitingForKeyPress) {
                long now = SystemTimer.getTime();
                boolean exists = hasBlackHole();

                if (!exists) {
                    if (nextBlackholeSpawn == 0) {
                        nextBlackholeSpawn = now + blackholeSpawnInterval;
                    }
                    if (now >= nextBlackholeSpawn) {
                        spawnBlackHole();
                        nextBlackholeSpawn = 0; // 스폰 후 카운트 중지
                    }
                } else {
                    nextBlackholeSpawn = 0; // 이미 있으면 카운트 ㄴㄴ
                }
            }

            //아이템 박스 스폰
            if (!waitingForKeyPress) {
                boolean itemExists = false;
                for (int i = 0; i < entities.size(); i++) {
                    if (entities.get(i) instanceof ItemBoxEntity) { itemExists = true; break; }
                }
                long now = SystemTimer.getTime();

                if (!itemExists) {
                    // 아이템이 없거나 타이머가 꺼져 있으면 지금부터 카운트 시작
                    if (nextItemSpawn == 0) {
                        nextItemSpawn = now + itemSpawnInterval;
                    }
                    // 카운트가 끝났으면 스폰하고 타이머 멈춤
                    if (now >= nextItemSpawn) {
                        spawnItemBoxOnPlayerPath();
                        nextItemSpawn = 0;
                    }
                } else {
                    // 아이템이 이미 존재하면 카운트 중지
                    nextItemSpawn = 0;
                }

                if (now - lastAlienFire >= alienShootingInterval) {
                    java.util.ArrayList shooters = new java.util.ArrayList();
                    for (int i = 0; i < entities.size(); i++) {
                        Entity e = (Entity) entities.get(i);
                        if (e instanceof AlienEntity) {
                            shooters.add(e);
                        }
                    }

                    if (!shooters.isEmpty()) {
                        // 무작위 에일리언 하나 선택
                        Entity shooter = (Entity) shooters.get(rng.nextInt(shooters.size()));

                        // 총알 스폰 위치
                        int sx = shooter.getX() + (shooter.getWidth() / 2) - 2; // 살짝 중앙 정렬
                        int sy = shooter.getY() + shooter.getHeight();

                        // 발사
                        entities.add(new AlienShotEntity(
                                this,
                                "sprites/shot.gif", // 없으면 "sprites/shot.gif"로 대체
                                sx,
                                sy
                        ));
                        lastAlienFire = now;
                    }
                }
            }

            if (!waitingForKeyPress && isLaserAutoActive()) {
                int shipCenterX = ship.getX() + ship.getWidth() / 2;
                int beamX = shipCenterX - beamWidth / 2;
                int beamTop = 0;
                int beamBottom = ship.getY();

                g.setColor(Color.red);
                g.fillRect(beamX, beamTop, beamWidth, Math.max(0, beamBottom - beamTop));

                int dmgThisFrame = Math.max(1, (int)Math.round(laserDps * (delta / 1000.0)));

                entities.add(new LaserEntity(
                        this,
                        beamX,
                        beamTop,
                        beamWidth,
                        Math.max(0, beamBottom - beamTop),
                        dmgThisFrame
                ));
            }


			
			// cycle round drawing all the entities we have in the game
			for (int i=0;i<entities.size();i++) {
				Entity entity = (Entity) entities.get(i);
				entity.draw(g);
			}

            drawHearts(g);

			// brute force collisions, compare every entity against
			// every other entity. If any of them collide notify 
			// both entities that the collision has occured
			for (int p=0;p<entities.size();p++) {
				for (int s=p+1;s<entities.size();s++) {
					Entity me = (Entity) entities.get(p);
					Entity him = (Entity) entities.get(s);
					
					if (me.collidesWith(him) || him.collidesWith(me)) {
						me.collidedWith(him);
						him.collidedWith(me);
					}
				}
			}
			


            for (int i = 0; i < entities.size(); i++) {
                Entity e = (Entity) entities.get(i);
                if (e instanceof LaserEntity) {
                    removeEntity(e);
                }
            }

            entities.removeAll(removeList);
            removeList.clear();

            if (blackHoleRespawnPending) {
                blackHoleRespawnPending = false;
                spawnBlackHole();
            }

			// if a game event has indicated that game logic should
			// be resolved, cycle round every entity requesting that
			// their personal logic should be considered.
			if (logicRequiredThisLoop) {
				for (int i=0;i<entities.size();i++) {
					Entity entity = (Entity) entities.get(i);
					entity.doLogic();
				}
				
				logicRequiredThisLoop = false;
			}
			
			// if we're waiting for an "any key" press then draw the 
			// current message 
			if (waitingForKeyPress) {
				g.setColor(Color.white);
				g.drawString(message,(VIRTUAL_WIDTH -g.getFontMetrics().stringWidth(message))/2,250);
				g.drawString("Press any key",(VIRTUAL_WIDTH-g.getFontMetrics().stringWidth("Press any key"))/2,300);
			}
			
			// finally, we've completed drawing so clear up the graphics
			// and flip the buffer over
			g.dispose();
			strategy.show();
			
			// resolve the movement of the ship. First assume the ship 
			// isn't moving. If either cursor key is pressed then
			// update the movement appropraitely
			ship.setHorizontalMovement(0);

            // 배 중심 좌표
            int shipCenterX = ship.getX() + ship.getWidth()/2;
            int shipCenterY = ship.getY() + ship.getHeight()/2;
            // 블랙홀 감쇠 배율(0.35~1.0)
            double slowMul = getBlackHoleSpeedMultiplier(shipCenterX, shipCenterY);
			
			if ((leftPressed) && (!rightPressed)) {
				ship.setHorizontalMovement(-moveSpeed * slowMul);
			} else if ((rightPressed) && (!leftPressed)) {
				ship.setHorizontalMovement(moveSpeed * slowMul);
			}
			
			// if we're pressing fire, attempt to fire
			if (firePressed) {
				tryToFire();
			}
			
			// we want each frame to take 10 milliseconds, to do this
			// we've recorded when we started the frame. We add 10 milliseconds
			// to this and then factor in the current time to give 
			// us our final value to wait for
			SystemTimer.sleep(lastLoopTime+10-SystemTimer.getTime());
		}
	}
	
	/**
	 * A class to handle keyboard input from the user. The class
	 * handles both dynamic input during game play, i.e. left/right 
	 * and shoot, and more static type input (i.e. press any key to
	 * continue)
	 * 
	 * This has been implemented as an inner class more through 
	 * habbit then anything else. Its perfectly normal to implement
	 * this as seperate class if slight less convienient.
	 * 
	 * @author Kevin Glass
	 */
	private class KeyInputHandler extends KeyAdapter {
		/** The number of key presses we've had while waiting for an "any key" press */
		private int pressCount = 1;
		
		/**
		 * Notification from AWT that a key has been pressed. Note that
		 * a key being pressed is equal to being pushed down but *NOT*
		 * released. Thats where keyTyped() comes in.
		 *
		 * @param e The details of the key that was pressed 
		 */
            public void keyPressed(KeyEvent e) {
			// if we're waiting for an "any key" typed then we don't 
			// want to do anything with just a "press"

			if (waitingForKeyPress) {
				return;
			}




			
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				leftPressed = true;
			}
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				rightPressed = true;
			}
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				firePressed = true;
			}
		} 
		
		/**
		 * Notification from AWT that a key has been released.
		 *
		 * @param e The details of the key that was released 
		 */
		public void keyReleased(KeyEvent e) {
			// if we're waiting for an "any key" typed then we don't 
			// want to do anything with just a "released"
			if (waitingForKeyPress) {
				return;
			}


			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				leftPressed = false;
			}
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				rightPressed = false;
			}
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				firePressed = false;
			}
		}

		/**
		 * Notification from AWT that a key has been typed. Note that
		 * typing a key means to both press and then release it.
		 *
		 * @param e The details of the key that was typed. 
		 */
		public void keyTyped(KeyEvent e) {
			// if we're waiting for a "any key" type then
			// check if we've recieved any recently. We may
			// have had a keyType() event from the user releasing
			// the shoot or move keys, hence the use of the "pressCount"
			// counter.
			if (waitingForKeyPress) {
				if (pressCount == 1) {
					// since we've now recieved our key typed
					// event we can mark it as such and start 
					// our new game
					waitingForKeyPress = false;
					startGame();
					pressCount = 0;
				} else {
					pressCount++;
				}
			}
			
			// if we hit escape, then quit the game
			if (e.getKeyChar() == 27) {
				System.exit(0);
			}
		}
	}

    private void spawnBoss() {
        if (bossSpawned) return;

        // 보스 등장 위치
        int bx = (VIRTUAL_WIDTH / 2) - 64;
        int by = 80;

        boss = new BossEntity(this, bx, by);
        entities.add(boss);

        bossSpawned = true;
    }

    public boolean isPlayerInvincible() {
        return SystemTimer.getTime() < invincibleUntil;
    }

    private void drawHearts(Graphics2D g) {
        int x = heartMarginX;
        int y = heartMarginY;

        for (int i = 0; i < maxLives; i++) {
            org.newdawn.spaceinvaders.Sprite spr = (i < lives) ? heartFull : heartEmpty;
            spr.draw(g, x, y);
            x += spr.getWidth() + heartSpacing; // 스프라이트 원본 폭 기준 간격
        }
    }

    //아이템 박스 중복 생성 방지
    private boolean hasItemBox() {
        for (int i = 0; i < entities.size(); i++) {
            if (entities.get(i) instanceof ItemBoxEntity) return true;
        }
        return false;
    }

    //아이템 박스가 생성될 경로
    private void spawnItemBoxOnPlayerPath() {
        if (ship == null) return;

        int y = ship.getY();
        int margin = 10;
        int boxW   = 32;
        int minX = margin;
        int maxX = VIRTUAL_WIDTH - margin - boxW;

        int shipLeft  = ship.getX();
        int shipRight = ship.getX() + ship.getWidth();

        java.util.Random rng = this.rng != null ? this.rng : new java.util.Random();

        int x = shipLeft;
        for (int tries = 0; tries < 20; tries++) {
            int candidate = rng.nextInt(maxX - minX + 1) + minX;
            boolean overlap = (candidate + boxW > shipLeft) && (candidate < shipRight);
            if (!overlap) { x = candidate; break; }
        }

        entities.add(new ItemBoxEntity(this, x, y));
    }

    public void throwBomb(int distancePx) {
        if (ship == null) return;
        int sx = ship.getX() + ship.getWidth()/2 - 8;
        int sy = ship.getY() - 20;

        double speed = -220;
        long fuseMs  = (long) Math.max(100, Math.round((distancePx / Math.abs(speed)) * 1000.0));

        entities.add(new BombEntity(
                this, "sprites/bomb.png", sx, sy,
                bombRadius, bombBaseDamage,
                speed, fuseMs
        ));
    }


    //폭발 시각 효과
    public void addExplosionEffect(int cx, int cy) {
        int ex = cx - 16;
        int ey = cy - 16;
        entities.add(new org.newdawn.spaceinvaders.entity.ExplosionEntity(this, ex, ey));
    }


    public void applyExplosion(int cx, int cy, int radius, int baseDamage) {
        int r2 = radius * radius;

        for (int i = 0; i < entities.size(); i++) {
            Entity e = (Entity) entities.get(i);

            // 각 엔티티의 중심 좌표
            int ex = e.getX() + e.getWidth()  / 2;
            int ey = e.getY() + e.getHeight() / 2;

            int dx = ex - cx;
            int dy = ey - cy;
            if (dx * dx + dy * dy > r2) continue; // 범위 밖

            if (e instanceof BossEntity) {
                BossEntity boss = (BossEntity) e;

            } else if (e instanceof AlienEntity) {
                ((AlienEntity) e).applyBombDamage(baseDamage);
            } else if (e instanceof AlienShotEntity) {
                removeEntity(e);
            }
        }
    }

    private boolean hasBlackHole() {
        for (int i = 0; i < entities.size(); i++) {
            if (entities.get(i) instanceof BlackHoleEntity) return true;
        }
        return false;
    }

    public void spawnBlackHole() {
        if (hasBlackHole() || ship == null) return;

        Sprite holeSpr = SpriteStore.get().getSprite("sprites/blackhole.gif");
        int holeW = holeSpr.getWidth();
        int holeH = holeSpr.getHeight();

        int marginTop = 0, marginBottom = 5; // 필요하면 여백 더 주세요
        int yLine = ship.getY();

        int y = yLine + ship.getHeight()/2 - holeH/2;

        y = Math.max(marginTop, Math.min(y, VIRTUAL_HEIGHT - holeH - marginBottom));


        int x =  ship.getX();; // 초기 임시값

        entities.add(new org.newdawn.spaceinvaders.entity.BlackHoleEntity(this, x, y));
    }

    private double getBlackHoleSpeedMultiplier(int sx, int sy) {
        double mul = 1.0;

        for (int i = 0; i < entities.size(); i++) {
            Entity e = (Entity) entities.get(i);
            if (!(e instanceof org.newdawn.spaceinvaders.entity.BlackHoleEntity)) continue;

            int cx = e.getX() + e.getWidth() / 2;
            int cy = e.getY() + e.getHeight() / 2;

            int dx = sx - cx;
            int dy = sy - cy;
            int dist2 = dx*dx + dy*dy;

            if (dist2 <= blackholeRadius * blackholeRadius) {
                mul = Math.min(mul, blackholeMultiplier);
            }
        }
        return mul;
    }

    private void spawnAsteroid() {
        org.newdawn.spaceinvaders.Sprite spr =
                org.newdawn.spaceinvaders.SpriteStore.get().getSprite("sprites/asteroid.png");
        int w = spr.getWidth();
        int x = rng.nextInt(Math.max(1, VIRTUAL_WIDTH - w));
        int y = -spr.getHeight();


        entities.add(new org.newdawn.spaceinvaders.entity.AsteroidEntity(this, x, y, 500));
    }

    public void loseHeart() {
        lives = Math.max(0, lives - 1);
        if (lives == 0) {
            notifyDeath();
        }
    }

    public void flagBlackHoleRespawn() { blackHoleRespawnPending = true; }

    /**
	 * The entry point into the game. We'll simply create an
	 * instance of class which will start the display and game
	 * loop.
	 * 
	 * @param argv The arguments that are passed into our game
	 */
	public static void main(String argv[]) {
		Game g = new Game();

		// Start the main game loop, note: this method will not
		// return until the game has finished running. Hence we are
		// using the actual main thread to run the game.
		g.gameLoop();
	}
}

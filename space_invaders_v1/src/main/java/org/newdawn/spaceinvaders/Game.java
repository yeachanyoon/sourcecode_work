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

    private boolean fullscreen = true; // 기본 전체화면 ON (원하면 false로 시작)
    public static final int VIRTUAL_WIDTH = 800;
    public static final int VIRTUAL_HEIGHT = 600;

    // 화면 정보
    private int screenW, screenH;
    private double scale;           // 가로/세로 중 작은 비율
    private int offsetX, offsetY; // 레터박스 오프셋(중앙정렬)

    private volatile boolean requestBomb = false;  // 키 입력으로 요청
    private long nextBombTime = 0;
    private long bombCooldownMs = 5000;            // 5초 쿨다운
    private int  bombRadius    = 120;              // 폭발 반경(px, 논리 좌표 기준)
    private int  bombBaseDamage = 200; // DamageInfo.damage(기본치)


    private boolean laserPressed = false;

    private int    laserDps        = 500;  // 초당 기본 피해(보스는 배율 8배 적용 → 체감 큼)
    private int    beamWidth     = 8;    // 픽


    /** 보스 스폰 여부 */
    private boolean bossSpawned = false;
    /** 현재 보스 참조(필요 시) */
    private BossEntity boss;
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
        computeScaleAndOffset();

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

        computeScaleAndOffset();

		// initialise the entities in our game so there's something
		// to see at startup
		initEntities();
	}
    private void computeScaleAndOffset() {
        // 논리(800x600) → 실제 화면으로 비율 유지 확대
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

    public void notifyBossKilled() {
        // 보스는 한 마리만 가정
        message = "보스를 죽이다니..ㄷㄷ 축하합니다!";
        waitingForKeyPress = true;
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

            // 3) 논리 캔버스를 지우기 (기존 800x600 → 상수 사용)
            g.setColor(Color.black);
            g.fillRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
			
			// cycle round asking each entity to move itself, 엔티티 이동
			if (!waitingForKeyPress) {
				for (int i=0;i<entities.size();i++) {
					Entity entity = (Entity) entities.get(i);
					
					entity.move(delta);
				}
			}

            if (!waitingForKeyPress && laserPressed) {
                // 배 중심에서 위쪽으로 뻗는 수직 빔
                int shipCenterX = ship.getX() + ship.getWidth() / 2;
                int beamX = shipCenterX - beamWidth / 2;
                int beamTop = 0;
                int beamBottom = ship.getY(); // 배 위쪽까지만 (아래쪽도 원하면 HEIGHT 사용)

                // 프레임 피해량 = DPS * (프레임 시간)
                int dmgThisFrame = Math.max(1, (int)Math.round(laserDps * (delta / 1000.0)));

                entities.add(new LaserEntity(
                        this,
                        beamX,
                        beamTop,
                        beamWidth,
                        Math.max(0, beamBottom - beamTop),
                        dmgThisFrame
                ));

                for (int i = 0; i < entities.size(); i++) {
                    Entity e = (Entity) entities.get(i);

                    // AABB로 레이저와 겹침 체크
                    boolean hitX = (e.getX() < beamX + beamWidth) && (e.getX() + e.getWidth() > beamX);
                    boolean hitY = (e.getY() < beamBottom) && (e.getY() + e.getHeight() > beamTop);
                    if (!(hitX && hitY)) continue;

                    if (e instanceof LaserEntity) {
                        removeEntity(e);  // removeList에 넣음
                    }

                        // 일반 외계인은 즉시 제거 + 카운트/가속 유지
                    if (e instanceof org.newdawn.spaceinvaders.entity.AlienEntity) {
                        removeEntity(e);
                        notifyAlienKilled();
                    }

                }
            }

            if (!waitingForKeyPress && requestBomb) {
                long now = SystemTimer.getTime();
                if (now >= nextBombTime) {
                    int cx = ship.getX() + ship.getWidth()/2; // 배 중심
                    int cy = ship.getY();
                    applyExplosion(cx, cy, bombRadius, bombBaseDamage);
                    nextBombTime = now + bombCooldownMs;
                }
                requestBomb = false;
            }
			
			// cycle round drawing all the entities we have in the game
			for (int i=0;i<entities.size();i++) {
				Entity entity = (Entity) entities.get(i);
				
				entity.draw(g);
			}

            if (laserPressed) {
                g.setColor(Color.red);
                int shipCenterX = ship.getX() + ship.getWidth() / 2;
                int beamX = shipCenterX - beamWidth / 2;
                int beamTop = 0;
                int beamBottom = ship.getY();
                g.fillRect(beamX, beamTop, beamWidth, Math.max(0, beamBottom - beamTop));
            }
			// brute force collisions, compare every entity against
			// every other entity. If any of them collide notify 
			// both entities that the collision has occured
			for (int p=0;p<entities.size();p++) {
				for (int s=p+1;s<entities.size();s++) {
					Entity me = (Entity) entities.get(p);
					Entity him = (Entity) entities.get(s);
					
					if (me.collidesWith(him)) {
						me.collidedWith(him);
						him.collidedWith(me);
					}
				}
			}
			
			// remove any entity that has been marked for clear up
			entities.removeAll(removeList);
			removeList.clear();

            for (int i = 0; i < entities.size(); i++) {
                Entity e = (Entity) entities.get(i);
                if (e instanceof LaserEntity) {
                    removeEntity(e);
                }
            }

            entities.removeAll(removeList);
            removeList.clear();

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
			
			if ((leftPressed) && (!rightPressed)) {
				ship.setHorizontalMovement(-moveSpeed);
			} else if ((rightPressed) && (!leftPressed)) {
				ship.setHorizontalMovement(moveSpeed);
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

            if (e.getKeyCode() == KeyEvent.VK_L) { // ← 레이저 키
                laserPressed = true;
            }


            if (e.getKeyCode() == KeyEvent.VK_B) {
                requestBomb = true;          // 바로 적용하지 말고 플래그만 세움 (스레드 안전)
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

            if (e.getKeyCode() == KeyEvent.VK_L) {
                laserPressed = false;
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

    public void applyExplosion(int cx, int cy, int radius, int baseDamage) {
        int r2 = radius * radius;

        for (int i = 0; i < entities.size(); i++) {
            Entity e = (Entity) entities.get(i);

            // 중심점 간 거리로 간단히 판정 (스프라이트 중심 사용)
            int ex = e.getX() + e.getWidth() / 2;
            int ey = e.getY() + e.getHeight() / 2;
            int dx = ex - cx;
            int dy = ey - cy;
            if (dx*dx + dy*dy > r2) continue; // 범위 밖

            if (e instanceof BossEntity) {
                BossEntity boss = (BossEntity) e;
                boss.applyBombDamage(baseDamage);
            } else if (e instanceof AlienEntity) {
                removeEntity(e);
                notifyAlienKilled();
            } else if (e instanceof AlienShotEntity) {
                removeEntity(e);
            }


        }

        // (선택) 시각 효과를 그리고 싶다면: 폭발 좌표/타이머를 필드로 저장하고 draw에서 원/플래시
    }

    private void spawnBoss() {
        if (bossSpawned) return;

        // 보스 등장 위치(중앙 상단 쯤). 필요하면 조정해도 됨.
        int bx = (VIRTUAL_WIDTH / 2) - 64;   // 보스 이미지 폭이 128px이라고 가정(대략치)
        int by = 80;

        boss = new BossEntity(this, bx, by); // BossEntity는 내부에서 "sprites/boss.gif" 사용
        entities.add(boss);

        bossSpawned = true;
        // (선택) 연출: 잠깐 메시지 띄우고 싶다면
        // message = "WARNING: BOSS APPROACHING!";
        // waitingForKeyPress = false; // 진행은 멈추지 않게
    }
	
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

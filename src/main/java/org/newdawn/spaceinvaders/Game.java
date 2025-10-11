package org.newdawn.spaceinvaders;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.ShipEntity;
import org.newdawn.spaceinvaders.entity.ShotEntity;
import org.newdawn.spaceinvaders.entity.GameState;
import org.newdawn.spaceinvaders.entity.PlayerState;
import org.newdawn.spaceinvaders.entity.AlienState;
//랭킹 스코어
//import org.newdawn.spaceinvaders.PlayerScore;
//import org.newdawn.spaceinvaders.RankingManager;
//깃허브 테스트용 주석
// SnakeYAML 라이브러리의 핵심 클래스
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
// 파일 입출력을 위한 자바 기본 클래스
import java.io.FileWriter;       // 파일에 쓸 때
import java.io.FileInputStream;  // 파일에서 읽어올 때
import java.io.InputStream;      // 스트림으로 데이터를 다룰 때
import java.io.IOException;// 입출력 예외 처리를 위해
import java.io.FileReader;
import java.io.FileWriter;
import java.util.stream.Collectors;

// YAML 데이터 구조를 담을 자바의 자료구조 클래스
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/*메뉴바 기능*/
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities; // 이 jpanel 에러 해결용 줄
//랭킹 스코어
import javax.swing.JOptionPane;
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
	private ArrayList<Entity> entities = new ArrayList<>();
	/** The list of entities that need to be removed from the game this loop */
	private ArrayList<Entity> removeList = new ArrayList<>();
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
//    변수 추가
    private int score; // 스코어 변수 추가
    private RankingManager rankingManager; // 랭킹 관리자 추가
//    기체 강화
    private int bulletCount = 1; // 현재 발사되는 탄 개수
    private boolean speedUpgradeApplied = false; // 속도 강화 적용 여부
    private boolean bulletCountUpgradeApplied = false; // 탄 개수 강화 적용 여부

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
    private int JuSuk;
    /** 저장경로 추가**/
    private final String SAVE_FILE_PATH = "savegame.yaml";
	/**
	 * Construct our game and set it running.
	 */
	public Game() {
		// create a frame to contain our game
		container = new JFrame("Space Invaders 102");
        rankingManager = new RankingManager(); // 랭킹 관리자 초기화



        // get hold the content of the frame and set up the resolution of the game
		JPanel panel = (JPanel) container.getContentPane();
		panel.setPreferredSize(new Dimension(800,600));
		panel.setLayout(null);

        // 1. 메뉴바 생성
        JMenuBar menuBar = new JMenuBar();

        // 2. "File" 메뉴 생성
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        // 3. "Save Game" 메뉴 아이템 생성 및 이벤트 연결
        JMenuItem saveItem = new JMenuItem("Save Game");
        saveItem.addActionListener(e -> saveGame()); // 람다식으로 saveGame() 메소드 호출
        fileMenu.add(saveItem);

        // 4. "Load Game" 메뉴 아이템 생성 및 이벤트 연결
        JMenuItem loadItem = new JMenuItem("Load Game");
        loadItem.addActionListener(e -> loadGame()); // 람다식으로 loadGame() 메소드 호출
        fileMenu.add(loadItem);

        // --- 랭킹 메뉴 아이템 추가 ---
        JMenuItem rankingItem = new JMenuItem("Ranking");
        rankingItem.addActionListener(e -> rankingManager.showRankingBoard(container));
        fileMenu.add(rankingItem);
        // -------------------------
        // 5. 생성된 메뉴바를 프레임(container)에 붙이기
        container.setJMenuBar(menuBar);

		// setup our canvas size and put it into the content of the frame
		setBounds(0,0,800,600);
		panel.add(this);
		
		// Tell AWT not to bother repainting our canvas since we're
		// going to do that our self in accelerated mode
		setIgnoreRepaint(true);
		
		// finally make the window visible 
		container.pack();
		container.setResizable(false);
		container.setVisible(true);
		
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
		
		// initialise the entities in our game so there's something
		// to see at startup
		initEntities();
	}
	
	/**
	 * Start a fresh game, this should clear out any old data and
	 * create a new set.
	 */
	private void startGame() {
		// clear out any existing entities and intialise a new set
		entities.clear();
		initEntities();
        score = 0; // 점수 초기화
        //기체 스테이터스
        moveSpeed = 300;
        firingInterval = 500;
        bulletCount = 1;
        speedUpgradeApplied = false;
        bulletCountUpgradeApplied = false;
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
        endGame("Oh no! They got you, try again?");
        //		message = "Oh no! They got you, try again?";
        //		waitingForKeyPress = true;
	}
	
	/**
	 * Notification that the player has won since all the aliens
	 * are dead.
	 */
	public void notifyWin() {
        endGame("Well done! You Win!");
//		message = "Well done! You Win!";
//		waitingForKeyPress = true;
	}
	
	/**
	 * Notification that an alien has been killed
	 */
	public void notifyAlienKilled() {
		// reduce the alient count, if there are none left, the player has won!
        score += 100; // 외계인 처치 시 100점 추가
		alienCount--;
		
		if (alienCount == 0) {
			notifyWin();
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
    // Game.java 내부에 새로운 메소드로 추가
    private void endGame(String winOrLoseMessage) {
        // UI 관련 작업을 안전하게 UI 스레드에서 실행하도록 예약합니다.
        SwingUtilities.invokeLater(() -> {
            String name = JOptionPane.showInputDialog(container, "Your Score: " + score + "\nEnter your name:", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            if (name != null && !name.trim().isEmpty()) {
                rankingManager.addScore(name, score);
            }
            // 이름 입력창이 완전히 닫힌 후 랭킹 보드를 보여줍니다.
            rankingManager.showRankingBoard(container);
        });

        message = winOrLoseMessage;
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
        if (bulletCount == 1) {
            ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", ship.getX() + 10, ship.getY() - 30);
            entities.add(shot);
        } else if (bulletCount >= 2) {
            // 탄 개수 강화 시, 두 발을 살짝 벌려서 발사
            ShotEntity shot1 = new ShotEntity(this, "sprites/shot.gif", ship.getX() + 2, ship.getY() - 30);
            ShotEntity shot2 = new ShotEntity(this, "sprites/shot.gif", ship.getX() + 18, ship.getY() - 30);
            entities.add(shot1);
            entities.add(shot2);
        }
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
    //강화 체크
    private void checkUpgrades() {
        // 2000점: 속도 강화
        if (score >= 2000 && !speedUpgradeApplied) {
            speedUpgradeApplied = true;
            moveSpeed *= 1.3; // 이동 속도 30% 증가
            firingInterval *= 0.7; // 연사 간격 30% 감소 (연사 속도 증가)
            message = "SPEED & FIRE RATE UP!";
            lastFire = System.currentTimeMillis() + 2000; // 2초간 메시지 표시
        }

        // 4000점: 탄 개수 강화
        if (score >= 4000 && !bulletCountUpgradeApplied) {
            bulletCountUpgradeApplied = true;
            bulletCount = 2; // 탄 개수 2개로 증가
            message = "MULTI-SHOT UPGRADE!";
            lastFire = System.currentTimeMillis() + 2000;
        }
    }

    public boolean isPendingRemoval(Entity entity) {
        return removeList.contains(entity);
    }

    public void gameLoop() {
		long lastLoopTime = SystemTimer.getTime();



		// keep looping round til the game ends
		while (gameRunning) {
			// work out how long its been since the last update, this
			// will be used to calculate how far the entities should
			// move this loop
            checkUpgrades();
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
			
			// Get hold of a graphics context for the accelerated 
			// surface and blank it out
			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			g.setColor(Color.black);
			g.fillRect(0,0,800,600);
			//점수판
            g.setColor(Color.white);
            g.drawString("Score: " + score, 10, 20);
			// cycle round asking each entity to move itself
			if (!waitingForKeyPress) {
				for (int i=0;i<entities.size();i++) {
					Entity entity = (Entity) entities.get(i);
					
					entity.move(delta);
				}
			}
			
			// cycle round drawing all the entities we have in the game
			for (int i=0;i<entities.size();i++) {
				Entity entity = (Entity) entities.get(i);
				
				entity.draw(g);
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
				g.drawString(message,(800-g.getFontMetrics().stringWidth(message))/2,250);
				g.drawString("Press any key",(800-g.getFontMetrics().stringWidth("Press any key"))/2,300);
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

    /*저장을 위한 메소드 두개*/
    public void saveGame() {
        try (FileWriter writer = new FileWriter(SAVE_FILE_PATH)) {
            GameState gameState = new GameState();
            gameState.alienCount = this.alienCount;
            gameState.score = this.score; // 현재 점수를 gameState에 저장
            // 기체 강화 저장
            gameState.moveSpeed = this.moveSpeed;
            gameState.firingInterval = this.firingInterval;
            gameState.bulletCount = this.bulletCount;
            gameState.speedUpgradeApplied = this.speedUpgradeApplied;
            gameState.bulletCountUpgradeApplied = this.bulletCountUpgradeApplied;
            gameState.playerState = new PlayerState(ship.getX(), ship.getY());
            gameState.alienStates = entities.stream()
                    .filter(e -> e instanceof AlienEntity)
                    .map(e -> {
                        AlienEntity alien = (AlienEntity) e;
                        return new AlienState(alien.getX(), alien.getY(), alien.getHorizontalMovement());
                    })
                    .collect(Collectors.toList());

            // =================  수정된 부분 시작 =================

            // 1. Representer를 생성하여 GameState 클래스를 일반 맵(MAP)으로 취급하도록 설정
            //    이렇게 하면 !!...GameState 태그가 파일에 쓰이지 않습니다.
            Representer representer = new Representer(new DumperOptions());
            representer.addClassTag(GameState.class, Tag.MAP);

            // 2. 이 Representer를 사용하여 Yaml 객체 생성
            Yaml yaml = new Yaml(representer);

            // =================  수정된 부분 끝 =================

            yaml.dump(gameState, writer);

            message = "Game Saved!";
            lastFire = System.currentTimeMillis() + 2000;
        } catch (IOException e) {
            message = "Error saving game: " + e.getMessage();
        }
    }

    public void loadGame() {
        try (FileReader reader = new FileReader(SAVE_FILE_PATH)) {

            // =================  수정된 부분 시작 =================

            // 가장 간단한 Yaml 객체와 loadAs 메소드를 사용합니다.
            Yaml yaml = new Yaml();
            GameState loadedState = yaml.loadAs(reader, GameState.class);

            // =================  수정된 부분 끝 =================
// 기체 강화
            // 불러온 상태 적용
            this.alienCount = loadedState.alienCount;
            this.score = loadedState.score;

            // 강화 상태 불러오기
            this.moveSpeed = loadedState.moveSpeed;
            this.firingInterval = loadedState.firingInterval;
            this.bulletCount = loadedState.bulletCount;
            this.speedUpgradeApplied = loadedState.speedUpgradeApplied;
            this.bulletCountUpgradeApplied = loadedState.bulletCountUpgradeApplied;

            if (loadedState == null) {
                message = "No save data found.";
                return;
            }

            entities.clear();
            this.alienCount = loadedState.alienCount;
            this.score = loadedState.score; // 불러온 점수를 현재 게임에 적용


            ship = new ShipEntity(this, "sprites/ship.gif", (int)loadedState.playerState.x, (int)loadedState.playerState.y);
            entities.add(ship);

            for (AlienState alienState : loadedState.alienStates) {
                AlienEntity alien = new AlienEntity(this, (int)alienState.x, (int)alienState.y);
                alien.setHorizontalMovement(alienState.dx);
                entities.add(alien);
            }

            message = "Game Loaded!";
            waitingForKeyPress = false;

        } catch (IOException e) {
            message = "Error loading game: " + e.getMessage();
        }
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

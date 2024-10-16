import tester.*;
import javalib.worldimages.*;
import javalib.funworld.*;
import java.awt.Color;
import java.util.Random;

//represents the game world
class MyWorld extends World {
 
  Random rand;
  PlayerFish playa;
  ILoFish bgfish;
  int tickClock;
  boolean won;
 
  MyWorld(Random rand) {
    this.rand = rand;
    this.playa = new PlayerFish(100, 100, 0, 0, 10, new FromFileImage("playerfish.png"));
    this.bgfish = new MtLoFish();
    this.tickClock = 0;
    this.won = false;
  }
 
  MyWorld() {
    this(new Random());
  }
  
  // alternate constructor taking in player and enemy fish as arguments
  MyWorld(Random rand, PlayerFish playa, ILoFish bgfish, int tickClock, boolean won) {
    this.rand = rand;
    this.playa = playa;
    this.bgfish = bgfish;
    this.tickClock = tickClock;
    this.won = won;
  }
  
  /*
   * fields
   * this.rand ... Random
   * this.playa ... Player
   * this.bgfish ... ILoFish
   * this.tickClock ... int
   * this.won ... boolean
   * 
   * Methods 
   * this.makeScene ... WorldScene
   * this.onTick ... World
   * this.onKeyEvent ... World
   * this.lastScene ... WorldScene
   * 
   * Methods of Fields
   */

  //method to create the scene for the game
  public javalib.funworld.WorldScene makeScene() {
    WorldScene ws = new WorldScene(800, 600)
        .placeImageXY(new FromFileImage("background.png"), 400, 300);
    ws = this.playa.draw(ws);
    ws = this.bgfish.drawAll(ws);
    return ws;
  }
  
  //method is called every tick to essentially update the world
  public World onTick() {
    // check win and loss conditions
    if (this.playa.size > this.bgfish.biggestFishSize()) {
      this.won = true;
      return this.endOfWorld("YOU WIN!");
    }
    else if (this.bgfish.canEat(this.playa)) {
      return this.endOfWorld("YOU GOT MUNCHED!");
    }
    else {
      // tick the clock
      this.tickClock++;
      
      // every 25 ticks, add a random fish (if there are less than 10 fish currently)
      if (this.tickClock % 25 == 0 && this.bgfish.countFish() < 10) {
        this.bgfish = this.bgfish.addRandomFish(this.playa, this.rand);
      }
      // update the player's movement, accounting for friction
      this.playa = this.playa.move().applyFriction();
      // update the background fish's movement, no friction
      this.bgfish = this.bgfish.moveAll();
      
      // grow the player by any enemy fish that can be eaten
      this.playa = this.playa.grow(this.bgfish.caloriesEaten(this.playa));
      // actually eat the enemy fish, filtering them out of the list
      this.bgfish = this.bgfish.filterEaten(this.playa);
     
      return this;
    }
  }
  
  //method is called every key event
  public World onKeyEvent(String key) {
    this.playa = this.playa.accel(key);
    return this;
  }
  
  // returns the last scene of the game depending if you win or lose
  // "YOU WIN!" or "YOU GOT EATED!"
  public WorldScene lastScene(String msg) {
    if (this.won) {
      return new WorldScene(800, 600).placeImageXY(new OverlayImage(new TextImage("YOU WIN!", 50,
          Color.black), new FromFileImage("background.png")), 400, 300);
    }
    else {
      return new WorldScene(800, 600).placeImageXY(new OverlayImage(
          new TextImage("YOU GOT EATED!", 50, Color.black), 
          new FromFileImage("background.png")), 400, 300);
    }
  }
 
}

//abstract class representing a fish
abstract class AFish {
  int posX;
  int posY;
  int dx;
  int dy;
  int size;
  WorldImage img;
  double friction;
  
  AFish(int posX, int posY, int dx, int dy, int size, WorldImage img) {
    this.posX = posX;
    this.posY = posY;
    this.dx = dx;
    this.dy = dy;
    this.size = size;
    this.img = img;
    this.friction = 0.9;
  }
  
  /*
   * Fields
   * this.posX ... int
   * this.posY ... int
   * this.dx ... int
   * this.dy ... int
   * this.size ... int
   * this.img ... WorldImage
   * this.friction .. double
   * 
   * Methods
   * this.draw(WorldScene) ... WorldScene
   * this.getDisplayX() ... int
   * this.getDisplayY() ... int
   * this.isBigger(int) ... boolean
   * this.checkCollision(AFish) ... boolean
   * this.render() ... WorldImage
   * this.move() ... AFish
   * this.makeRandomFish(Random) ... AFish
   * 
   * Methods of Fields
   */
  
  //draws the world scene
  public WorldScene draw(WorldScene canvas) {
    return canvas.placeImageXY(this.render(), this.getDisplayX(), this.getDisplayY());
  }
  
  //returns proper coordinate to be displayed based on the x position
  public int getDisplayX() {
    if (this.posX < 0) {
      return 800 + (this.posX % 800);
    }
    else {
      return this.posX % 800;
    }
  }
  
  //returns proper coordinate to be displayed based on the y position
  public int getDisplayY() {
    if (this.posY < 0) {
      return 600 + (this.posY % 600);
    }
    else {
      return this.posY % 600;
    }
  }
  
  //checks if this fish is bigger than the other fish
  boolean isBigger(AFish other) {
    return this.size > other.size;
  }
  
  //checks for a collision between this AFish and the given one
  boolean checkCollision(AFish other) {
    return Math.abs(this.getDisplayX() - other.getDisplayX()) < (this.size * 2 + other.size * 2)
        && Math.abs(this.getDisplayY() - other.getDisplayY()) < 
        (this.size * 1.25 + other.size * 1.25);
  }
  
  
  // renders this AFish's image to be displayed
  public WorldImage render() {
    double scale = (double)this.size / 10;
    return new ScaleImage(this.img, scale);
  }
  
  // moves the fish
  public AFish move() {
    this.posX += dx;
    this.posY += dy;
    return this;
  }

  // creates a new random enemy fish in the game
  public AFish makeRandomFish(Random rand) {
    AFish rando = new EnemyFish(rand.nextInt(800),
        rand.nextInt(600),
        rand.nextInt(30) - 15, // dx is between -15 and 15
        rand.nextInt(30) - 15, // dy is between -15 and 15
        rand.nextInt(30) + 5, // enemy size ranges from 5 to 35
        new FromFileImage("enemyfish.png"));
    
    if (this.checkCollision(rando)) {
      return this.makeRandomFish(rand);
    }
    else {
      return rando;
    }
  }
  
}


//represents the player fish
class PlayerFish extends AFish {

  PlayerFish(int posX, int posY, int dx, int dy, int size, WorldImage img) {
    super(posX, posY, dx, dy, size, img);
  }
  
  /*
   * Fields
   * this.posX ... int
   * this.posY ... int
   * this.dx ... int
   * this.dy ... int
   * this.size ... int
   * this.img ... WorldImage
   * 
   * Methods
   * this.accel(String) ... PlayerFish
   * this.grow() ... PlayerFish
   * this.move() ... PlayerFish
   * this.applyFriction() ... PlayerFish
   * 
   * 
   * Methods of Fields
   */

  //changes velocity of the player fish depending the key input
  public PlayerFish accel(String key) {
    double force = 80.0 / this.size;
    
    if (key.equals("left")) {
      this.dx -= force;
    }
    else if (key.equals("right")) {
      this.dx += force;
    }
    else if (key.equals("down")) {
      this.dy += force;
    }
    else if (key.equals("up")) {
      this.dy -= force;
    }
    return this;
  }

  //moves this PlayerFish
  public PlayerFish move() {
    this.posX += dx;
    this.posY += dy;
    return this;
  }
  
  // grows this PlayerFish by half of the consumed enemy's size
  PlayerFish grow(int amount) {
    this.size += (amount / 2);
    return this;
  }
  
  //applies friction to the movement of the player fish (use this for inertia) 
  PlayerFish applyFriction() {
    this.dx *= this.friction;
    this.dy *= this.friction;
    return this;
  }
  
}

//represents enemy fish
class EnemyFish extends AFish {

  EnemyFish(int posX, int posY, int dx, int dy, int size, WorldImage img) {
    super(posX, posY, dx, dy, size, img);
  }
  
  /*
   * Fields
   * this.posX ... int
   * this.posY ... int
   * this.dx ... int
   * this.dy ... int
   * this.size ... int
   * this.img ... WorldImage
   * 
   * Methods
   * isBigger() ... boolean
   * this.render() ... WorldImage
   * 
   * Methods of Fields
   */
  
  // moves the enemy fish depending on the its dx and dy
  public EnemyFish move() {
    this.posX += dx;
    this.posY += dy;
    return this;
  }

  
}

//represents a list of fish
interface ILoFish {

  //returns a world scene with all background fish drawn onto it
  WorldScene drawAll(WorldScene canvas);
  
  // adds a new random background fish to the list
  ILoFish addRandomFish(AFish playa, Random rand);

  ///returns the size of the biggest fish
  int biggestFishSize();

  // moves all the background fish
  ILoFish moveAll();
  
  //filters out all eaten fish
  ILoFish filterEaten(AFish player);
  
  // if player is bigger and collides, the player gets bigger
  int caloriesEaten(AFish player);
  
  // counts the number of fish in the list
  int countFish();
  
  //checks if the given player fish can be eaten by this list of fish
  boolean canEat(AFish player);
  
}

//represents an nonempty list of fish
class ConsLoFish implements ILoFish {
  
  AFish first;
  ILoFish rest;
  
  ConsLoFish(AFish first, ILoFish rest) {
    this.first = first;
    this.rest = rest;
  }
  
  /*
   * Fields
   * this.first ... AFish
   * this.rest ... IList<EnemyFish>
   * 
   * Methods
   * this.drawAll(WorldScene) ... WorldScene
   * this.moveAll()... ILoFish
   * this.filterEaten(AFish) ... ILoFish
   * this.caloriesEaten(AFish player) ... int
   * this.biggestFishSize() ... int
   * 
   * Methods of Fields
   */

  //draws this list of fish onto the given WorldScene
  public WorldScene drawAll(WorldScene canvas) {
    return this.rest.drawAll(canvas.placeImageXY(this.first.render(), 
        this.first.getDisplayX(), 
        this.first.getDisplayY()));
  }

  // moves this list of fish
  public ILoFish moveAll() {
    return new ConsLoFish(this.first.move(), this.rest.moveAll());
  }

  //filters out all eaten fish from this list
  // if the given player collides with it and the player is the bigger fish, it is eaten
  public ILoFish filterEaten(AFish player) {
    if (player.checkCollision(this.first) && player.isBigger(this.first)) {
      return this.rest.filterEaten(player);
    }
    else {
      return new ConsLoFish(this.first, this.rest.filterEaten(player));
    }
  }

  // compute the total size of the eaten fish in this list
  public int caloriesEaten(AFish player) {
    if (player.checkCollision(this.first) && player.isBigger(this.first)) {
      return this.first.size;
    }
    else {
      return this.rest.caloriesEaten(player);
    }
  }

  // finds the size of the biggest fish size in the list
  public int biggestFishSize() {
    return Math.max(this.first.size, this.rest.biggestFishSize());
  }

  // adds a new random fish to the front of this list
  public ILoFish addRandomFish(AFish playa, Random rand) {
    return new ConsLoFish(playa.makeRandomFish(rand), this);
  }

  // counts the number of fish in the list
  public int countFish() {
    return 1 + this.rest.countFish();
  }

  // checks if the given player fish can be eaten by this list of fish
  public boolean canEat(AFish player) {
    return (this.first.checkCollision(player) && this.first.isBigger(player)) 
        || this.rest.canEat(player);
  }
  
}

class MtLoFish implements ILoFish {
  
  MtLoFish(){}
  
  /*
   * Fields
   *  N/A
   * 
   * Methods
   * this.drawAll(WorldScene) ... WorldScene
   * this.moveAll() ... ILoFish
   * this.filterEaten ... ILoFish
   * this.caloriesEaten(AFish)... int
   * this.biggestFishSize() ... int
   * this.addRandomFish(AFish, Random) ... ILoFish
   * this.countFish() ... int
   * this.canEat(AFish) ... boolean
   * 
   * Methods of Fields
   */

  //draws this list of fish onto the given WorldScene
  public WorldScene drawAll(WorldScene canvas) {
    return canvas;
  }
  
  // moves this list of fish
  public ILoFish moveAll() {
    return this;
  }
  
  // filters out all of the eaten fish in the list 
  public ILoFish filterEaten(AFish player) {
    return this;
  }

  // compute the total size of the eaten fish in this list
  public int caloriesEaten(AFish player) {
    return 0;
  }

  // finds the size of the biggest fish size in the list
  public int biggestFishSize() {
    return 0;
  }

  // adds a new random fish to the front of this list
  public ILoFish addRandomFish(AFish playa, Random rand) {
    return new ConsLoFish(playa.makeRandomFish(rand), new MtLoFish());
  }

  //counts the number of fish in the list
  public int countFish() {
    return 0;
  }

  // checks if the given player fish can be eaten by this list of fish
  public boolean canEat(AFish player) {
    return false;
  }
 
}

//examples of worlds and tests for functions
class ExamplesMyWorld {
  
  Random rand = new Random(0);
  
  PlayerFish p1 = new PlayerFish(650, 580, 5, -5, 10, new FromFileImage("playerfish.png"));
  PlayerFish p2 = new PlayerFish(350, 400, -6, 2, 35, new FromFileImage("playerfish.png"));
  PlayerFish p3 = new PlayerFish(50, 5, 10, 3, 5, new FromFileImage("playerfish.png"));
  
  EnemyFish e1 = new EnemyFish(40, 1, 5, 5, 10, new FromFileImage("enemyfish.png"));
  EnemyFish e2 = new EnemyFish(200, 20, 15, 0, 15, new FromFileImage("enemyfish.png"));
  EnemyFish e3 = new EnemyFish(50, 50, 2, 12, 35, new FromFileImage("enemyfish.png"));
  EnemyFish e4 = new EnemyFish(812, -1100, 5, -14, 20, new FromFileImage("enemyfish.png"));
  EnemyFish e5 = new EnemyFish(500, 100, -4, 13, 5, new FromFileImage("enemyfish.png"));
  EnemyFish e6 = new EnemyFish(400, 500, 5, 5, 20, new FromFileImage("enemyfish.png"));
  EnemyFish e7 = new EnemyFish(-200, 900, -5, 5, 9, new FromFileImage("enemyfish.png"));
  
  World w1 = new MyWorld(new Random(0), 
      new PlayerFish(650, 580, 5, -5, 10, new FromFileImage("playerfish.png")), 
      new MtLoFish(), 0, false);
  World w2 = new MyWorld(new Random(0), 
      new PlayerFish(350, 400, -6, 2, 35, new FromFileImage("playerfish.png")), 
      new ConsLoFish(this.e1, new ConsLoFish(this.e2, new ConsLoFish(this.e3, this.mt))), 
      0, false);
  World w3 = new MyWorld(new Random(0), 
      new PlayerFish(50, 5, 10, 3, 5, new FromFileImage("playerfish.png")), 
      new ConsLoFish(this.e4, new ConsLoFish(this.e5, new ConsLoFish(this.e6, 
          new ConsLoFish(this.e7, this.mt)))), 0, false);
  
  World proper = new MyWorld(new Random(0), 
      new PlayerFish(350, 400, -6, 2, 35, new FromFileImage("playerfish.png")), 
      new ConsLoFish(new EnemyFish(40, 1, 5, 5, 10, new FromFileImage("enemyfish.png")), 
          new ConsLoFish(new EnemyFish(200, 20, 15, 0, 15, new FromFileImage("enemyfish.png")), 
              new ConsLoFish(new EnemyFish(50, 50, 2, 12, 35, new FromFileImage("enemyfish.png")),
                  new MtLoFish()))), 0, false);
  
  
  ILoFish mt = new MtLoFish();
  ILoFish lf1 = new ConsLoFish(this.e1, new ConsLoFish(this.e2, new ConsLoFish(this.e3, this.mt)));
  ILoFish lf2 = new ConsLoFish(this.e4, new ConsLoFish(this.e5, new ConsLoFish(this.e6, 
      new ConsLoFish(this.e7, this.mt))));
  
 
  boolean testBigBang(Tester t) {
    MyWorld w = new MyWorld(new Random(0), this.p1, this.lf1, 0, false);
    int worldWidth = 800;
    int worldHeight = 600;
    double tickRate = 0.05;
    return w.bigBang(worldWidth, worldHeight, tickRate);
  }
  
  boolean testCheckCollision(Tester t) {
    return true;
  }
  
  boolean testMakeRandomFish(Tester t) {
    Random rand1 = new Random(0);
    return t.checkExpect(new PlayerFish(650, 580, 5, -5, 10, new FromFileImage("playerfish.png"))
        .makeRandomFish(rand1), 
        new EnemyFish(560, 148, 4, 2, 10, new FromFileImage("enemyfish.png")))
        && t.checkExpect(new PlayerFish(350, 400, -6, 2, 35, new FromFileImage("playerfish.png"))
            .makeRandomFish(rand1), 
            new EnemyFish(253, 491, 6, -6, 19, new FromFileImage("enemyfish.png")))
        && t.checkExpect(new PlayerFish(50, 5, 10, 3, 5, new FromFileImage("playerfish.png"))
            .makeRandomFish(rand1), 
            new EnemyFish(277, 77, 8, 7, 10, new FromFileImage("enemyfish.png")));
  }
  
  boolean testDraw(Tester t) {
    return t.checkExpect(new PlayerFish(650, 580, 5, -5, 10, new FromFileImage("playerfish.png"))
        .draw(new WorldScene(800, 600)), 
        new WorldScene(800, 600).placeImageXY(new ScaleImage(
            new FromFileImage("playerfish.png"), 1.0), 650, 580))
        && t.checkExpect(new EnemyFish(40, 1, 5, 5, 10, new FromFileImage("enemyfish.png"))
            .draw(new WorldScene(800, 600)), 
            new WorldScene(800, 600).placeImageXY(new ScaleImage(
                new FromFileImage("enemyfish.png"), 1.0), 40, 1))
        && t.checkExpect(new EnemyFish(812, -1100, 5, -14, 20, new FromFileImage("enemyfish.png"))
            .draw(new WorldScene(800, 600).placeImageXY(new FromFileImage("background.png"), 
                400, 200)),
            new WorldScene(800, 600).placeImageXY(new FromFileImage("background.png"), 400, 200)
            .placeImageXY(new ScaleImage(new FromFileImage("enemyfish.png"), 2.0), 12, 100));
  }
  
  boolean testGetDisplayX(Tester t) {
    return t.checkExpect(new PlayerFish(650, 580, 5, -5, 10, new FromFileImage("playerfish.png"))
        .getDisplayX(), 650)
        && t.checkExpect(this.e1.getDisplayX(), 40)
        && t.checkExpect(this.e4.getDisplayX(), 12)
        && t.checkExpect(this.e7.getDisplayX(), 600);
  }
  
  boolean testGetDisplayY(Tester t) {
    return t.checkExpect(this.p3.getDisplayY(), 5)
        && t.checkExpect(this.e6.getDisplayY(), 500)
        && t.checkExpect(this.e4.getDisplayY(), 100)
        && t.checkExpect(this.e7.getDisplayY(), 300);
  }
  
  boolean testIsBigger(Tester t) {
    return t.checkExpect(this.p2.isBigger(this.p1), true)
        && t.checkExpect(this.e3.isBigger(this.p3), true)
        && t.checkExpect(this.e6.isBigger(this.e4), false)
        && t.checkExpect(this.p1.isBigger(this.e3), false);
  }
  
  boolean testMove(Tester t) {
    return t.checkExpect(new PlayerFish(650, 580, 5, -5, 17, 
        new FromFileImage("playerfish.png")).move(), 
        new PlayerFish(655, 575, 5, -5, 17, new FromFileImage("playerfish.png")))
        && t.checkExpect(new PlayerFish(350, 400, -6, 2, 35, 
            new FromFileImage("playerfish.png")).move(), 
            new PlayerFish(344, 402, -6, 2, 35, new FromFileImage("playerfish.png")))
        && t.checkExpect(new EnemyFish(20, 20, 15, 0, 15, 
            new FromFileImage("enemyfish.png")).move() , 
            new EnemyFish(35, 20, 15, 0, 15, new FromFileImage("enemyfish.png")))
        && t.checkExpect(new EnemyFish(812, -1100, 5, -14, 20, 
            new FromFileImage("enemyfish.png")).move(), 
            new EnemyFish(817, -1114, 5, -14, 20, new FromFileImage("enemyfish.png")));
  }
  
  boolean testRender(Tester t) {
    return t.checkExpect(this.p1.render(), 
        new ScaleImage(new FromFileImage("playerfish.png"), 1.0))
        && t.checkExpect(this.p2.render(), 
            new ScaleImage(new FromFileImage("playerfish.png"), 3.5))
        && t.checkExpect(this.e6.render(), 
            new ScaleImage(new FromFileImage("enemyfish.png"), 2.0))
        && t.checkExpect(this.e7.render(), 
            new ScaleImage(new FromFileImage("enemyfish.png"), 0.9));
  }
  
  boolean testAddRandomFish(Tester t) {
    Random rand = new Random(0);
    return t.checkExpect(this.mt.addRandomFish(new PlayerFish(650, 580, 5, -5, 17, 
        new FromFileImage("playerfish.png")), rand), 
        new ConsLoFish(new EnemyFish(560, 148, 4, 2, 10, 
            new FromFileImage("enemyfish.png")), this.mt))
        && t.checkExpect(new ConsLoFish(new EnemyFish(40, 1, 5, 5, 10, 
            new FromFileImage("enemyfish.png")), 
            new ConsLoFish(new EnemyFish(200, 20, 15, 0, 15, new FromFileImage("enemyfish.png")),
                new ConsLoFish(new EnemyFish(50, 50, 2, 12, 35, 
                    new FromFileImage("enemyfish.png")), this.mt)))
            .addRandomFish(new PlayerFish(650, 580, 5, -5, 17, 
                new FromFileImage("playerfish.png")), rand), 
            new ConsLoFish(new EnemyFish(253, 491, 6, -6, 19, new FromFileImage("enemyfish.png")),
                new ConsLoFish(new EnemyFish(40, 1, 5, 5, 10, new FromFileImage("enemyfish.png")),
                    new ConsLoFish(new EnemyFish(200, 20, 15, 0, 15, 
                        new FromFileImage("enemyfish.png")), 
                        new ConsLoFish(new EnemyFish(50, 50, 2, 12, 35, 
                            new FromFileImage("enemyfish.png")), this.mt)))));
  }
  
  boolean testBiggestFishSize(Tester t) {
    return t.checkExpect(this.mt.biggestFishSize(), 0)
        && t.checkExpect(this.lf1.biggestFishSize(), 35)
        && t.checkExpect(this.lf2.biggestFishSize(), 20);
  }
  
  boolean testCaloriesEaten(Tester t) {
    return t.checkExpect(this.mt.caloriesEaten(this.p1), 0)
        && t.checkExpect(this.lf1.caloriesEaten(this.p3), 0)
        && t.checkExpect(this.lf1.caloriesEaten(new PlayerFish(55, 52, 4, -3, 35, 
            new FromFileImage("playerfish.png"))), 10)
        && t.checkExpect(this.lf1.caloriesEaten(new PlayerFish(55, 80, 1, -8, 35, 
            new FromFileImage("playerfish.png"))), 0)
        && t.checkExpect(this.lf2.caloriesEaten(new PlayerFish(602, 298, 10, 3, 30, 
            new FromFileImage("playerfish.png"))), 9)
        && t.checkExpect(this.lf2.caloriesEaten(new PlayerFish(602, 298, 10, 3, 5, 
            new FromFileImage("playerfish.png"))), 0);
  }
  
  boolean testCanEat(Tester t) {
    return t.checkExpect(this.mt.canEat(p1), false)
        && t.checkExpect(this.lf1.canEat(p3), true) 
        && t.checkExpect(this.lf1.canEat(p2), false)
        && t.checkExpect(this.lf2.canEat(new PlayerFish(12, -500, 5, -14, 20, 
            new FromFileImage("playerfish.png"))), false)
        && t.checkExpect(this.lf2.canEat(new PlayerFish(10, 100, 5, -14, 15, 
            new FromFileImage("playerfish.png"))), true)
        && t.checkExpect(this.lf2.canEat(new PlayerFish(812, -1100, 5, -14, 25, 
            new FromFileImage("playerfish.png"))), false);
  }
  
  boolean testCountFish(Tester t) {
    return t.checkExpect(this.mt.countFish(), 0) 
        && t.checkExpect(this.lf1.countFish(), 3)
        && t.checkExpect(this.lf2.countFish(), 4); 
  }
  
  boolean testDrawAll(Tester t) {
    return t.checkExpect(this.mt.drawAll(new WorldScene(800, 600)), new WorldScene(800, 600))
        
        && t.checkExpect(
            new ConsLoFish(new EnemyFish(40, 1, 5, 5, 10, new FromFileImage("enemyfish.png")), 
                new ConsLoFish(new EnemyFish(200, 20, 15, 0, 15, 
                    new FromFileImage("enemyfish.png")),
                    new ConsLoFish(new EnemyFish(50, 50, 2, 12, 35, 
                        new FromFileImage("enemyfish.png")), this.mt)))
            .drawAll(new WorldScene(800, 600)), 
            new WorldScene(800, 600).placeImageXY(new ScaleImage(
                new FromFileImage("enemyfish.png"), 1.0), 40, 1)
            .placeImageXY(new ScaleImage(new FromFileImage("enemyfish.png"), 1.5), 200, 20)
            .placeImageXY(new ScaleImage(new FromFileImage("enemyfish.png"), 3.5), 50, 50))
        
        && t.checkExpect(new ConsLoFish(new EnemyFish(812, -1100, 5, -14, 20,
            new FromFileImage("enemyfish.png")), 
            new ConsLoFish(new EnemyFish(500, 100, -4, 13, 5, new FromFileImage("enemyfish.png")),
                new ConsLoFish(new EnemyFish(400, 500, 5, 5, 20, 
                    new FromFileImage("enemyfish.png")), 
                    new ConsLoFish(new EnemyFish(-200, 900, -5, 5, 9, 
                        new FromFileImage("enemyfish.png")), this.mt))))
            .drawAll(new WorldScene(800, 600).placeImageXY(
                new FromFileImage("background.png"), 400, 200)), 
            
            new WorldScene(800, 600).placeImageXY(new FromFileImage("background.png"), 400, 200)
            .placeImageXY(new ScaleImage(new FromFileImage("enemyfish.png"), 2.0), 12, 100)
            .placeImageXY(new ScaleImage(new FromFileImage("enemyfish.png"), 0.5), 500, 100)
            .placeImageXY(new ScaleImage(new FromFileImage("enemyfish.png"), 2.0), 400, 500)
            .placeImageXY(new ScaleImage(new FromFileImage("enemyfish.png"), 0.9), 600, 300));
  }
  
  boolean testFilterEaten(Tester t) {
    return t.checkExpect(this.mt.filterEaten(this.p1), this.mt)
        && t.checkExpect(this.lf1.filterEaten(this.p3), this.lf1)
        && t.checkExpect(this.lf1.filterEaten(new PlayerFish(55, 52, 4, -3, 35,
            new FromFileImage("playerfish.png"))),
            new ConsLoFish(this.e2, new ConsLoFish(this.e3, this.mt)))
        && t.checkExpect(this.lf1.filterEaten(new PlayerFish(55, 80, 1, -8, 35, 
            new FromFileImage("playerfish.png"))), this.lf1)
        && t.checkExpect(this.lf2.filterEaten(new PlayerFish(602, 298, 10, 3, 30, 
            new FromFileImage("playerfish.png"))), 
            new ConsLoFish(this.e4, new ConsLoFish(this.e5, new ConsLoFish(this.e6, this.mt))))
        && t.checkExpect(this.lf2.filterEaten(new PlayerFish(602, 298, 10, 3, 5, 
            new FromFileImage("playerfish.png"))), this.lf2);
  }
  
  boolean testMoveAll(Tester t) {
    return t.checkExpect(this.mt.moveAll(), this.mt)
        && t.checkExpect(new ConsLoFish(new EnemyFish(40, 1, 5, 5, 10,
            new FromFileImage("enemyfish.png")), 
            new ConsLoFish(new EnemyFish(200, 20, 15, 0, 15,
                new FromFileImage("enemyfish.png")), 
                new ConsLoFish(new EnemyFish(50, 50, 2, 12, 35,
                    new FromFileImage("enemyfish.png")), this.mt))).moveAll(), 
            new ConsLoFish(new EnemyFish(45, 6, 5, 5, 10, new FromFileImage("enemyfish.png")),
                new ConsLoFish(new EnemyFish(215, 20, 15, 0, 15,
                    new FromFileImage("enemyfish.png")),
                    new ConsLoFish(new EnemyFish(52, 62, 2, 12, 35,
                        new FromFileImage("enemyfish.png")), this.mt))))
        && t.checkExpect(new ConsLoFish(new EnemyFish(812, -1100, 5, -14, 20,
            new FromFileImage("enemyfish.png")), 
            new ConsLoFish(new EnemyFish(500, 100, -4, 13, 5, new FromFileImage("enemyfish.png")),
                new ConsLoFish(new EnemyFish(400, 500, 5, 5, 20, 
                    new FromFileImage("enemyfish.png")), 
                    new ConsLoFish(new EnemyFish(-200, 900, -5, 5, 9, 
                        new FromFileImage("enemyfish.png")), this.mt)))).moveAll(), 
            new ConsLoFish(new EnemyFish(817, -1114, 5, -14, 20, 
                new FromFileImage("enemyfish.png")), 
                new ConsLoFish(new EnemyFish(496, 113, -4, 13, 5, 
                    new FromFileImage("enemyfish.png")), 
                    new ConsLoFish(new EnemyFish(405, 505, 5, 5, 20, 
                        new FromFileImage("enemyfish.png")), 
                        new ConsLoFish(new EnemyFish(-205, 905, -5, 5, 9, 
                            new FromFileImage("enemyfish.png")), this.mt)))));
  }
  
  boolean testAccel(Tester t) {
    return t.checkExpect(new PlayerFish(650, 580, 5, -5, 10, 
        new FromFileImage("playerfish.png")).accel("up"), 
        new PlayerFish(650, 580, 5, -13, 10, new FromFileImage("playerfish.png")))
        && t.checkExpect(new PlayerFish(650, 580, 5, -5, 10, 
            new FromFileImage("playerfish.png")).accel("down"), 
            new PlayerFish(650, 580, 5, 3, 10, new FromFileImage("playerfish.png")))
        && t.checkExpect(new PlayerFish(650, 580, 5, -5, 10, 
            new FromFileImage("playerfish.png")).accel("right"), 
            new PlayerFish(650, 580, 13, -5, 10, new FromFileImage("playerfish.png")))
        && t.checkExpect(new PlayerFish(650, 580, 5, -5, 10, 
            new FromFileImage("playerfish.png")).accel("left"), 
            new PlayerFish(650, 580, -3, -5, 10, new FromFileImage("playerfish.png")))
        
        && t.checkExpect(new PlayerFish(350, 400, -6, 2, 35, 
            new FromFileImage("playerfish.png")).accel("up"), 
            new PlayerFish(350, 400, -6, 0, 35, new FromFileImage("playerfish.png")))
        && t.checkExpect(new PlayerFish(350, 400, -6, 2, 35, 
            new FromFileImage("playerfish.png")).accel("down"), 
            new PlayerFish(350, 400, -6, 4, 35, new FromFileImage("playerfish.png")))
        && t.checkExpect(new PlayerFish(350, 400, -6, 2, 35, 
            new FromFileImage("playerfish.png")).accel("right"), 
            new PlayerFish(350, 400, -3, 2, 35, new FromFileImage("playerfish.png")))
        && t.checkExpect(new PlayerFish(350, 400, -6, 2, 35, 
            new FromFileImage("playerfish.png")).accel("left"), 
            new PlayerFish(350, 400, -8, 2, 35, new FromFileImage("playerfish.png")));
  }
  
  boolean testApplyFriction(Tester t) {
    return t.checkExpect(new PlayerFish(650, 580, 5, -5, 10, 
        new FromFileImage("playerfish.png")).applyFriction(), 
        new PlayerFish(650, 580, 4, -4, 10, new FromFileImage("playerfish.png")))
        && t.checkExpect(new PlayerFish(350, 400, -6, 2, 35, 
            new FromFileImage("playerfish.png")).applyFriction(), 
            new PlayerFish(350, 400, -5, 1, 35, new FromFileImage("playerfish.png")))
        && t.checkExpect(new PlayerFish(50, 5, 10, 3, 5, 
            new FromFileImage("playerfish.png")).applyFriction(), 
            new PlayerFish(50, 5, 9, 2, 5, new FromFileImage("playerfish.png")));
  }
  
  boolean testGrow(Tester t) {
    return t.checkExpect(new PlayerFish(650, 580, 5, -5, 10, 
        new FromFileImage("playerfish.png")).grow(30), 
        new PlayerFish(650, 580, 5, -5, 25, new FromFileImage("playerfish.png")))
        && t.checkExpect(new PlayerFish(350, 400, -6, 2, 35, 
            new FromFileImage("playerfish.png")).grow(13), 
            new PlayerFish(350, 400, -6, 2, 41, new FromFileImage("playerfish.png")))
        && t.checkExpect(new PlayerFish(50, 5, 10, 3, 5, 
            new FromFileImage("playerfish.png")).grow(4), 
            new PlayerFish(50, 5, 10, 3, 7, new FromFileImage("playerfish.png")));
  }
  
  boolean testOnKeyEvent(Tester t) {
    return t.checkExpect(new MyWorld(new Random(0), 
        new PlayerFish(650, 580, 5, -5, 10, new FromFileImage("playerfish.png")), 
        new MtLoFish(), 0, false).onKeyEvent("up"), 
        new MyWorld(new Random(0),
            new PlayerFish(650, 580, 5, -13, 10, new FromFileImage("playerfish.png")), 
            new MtLoFish(), 0, false))
        
         && t.checkExpect(new MyWorld(new Random(0), 
             new PlayerFish(350, 400, -6, 2, 35, new FromFileImage("playerfish.png")), 
             new ConsLoFish(new EnemyFish(40, 1, 5, 5, 10, new FromFileImage("enemyfish.png")),
                 new ConsLoFish(new EnemyFish(200, 20, 15, 0, 15, 
                     new FromFileImage("enemyfish.png")), 
                     new ConsLoFish(new EnemyFish(50, 50, 2, 12, 35, 
                         new FromFileImage("enemyfish.png")), new MtLoFish()))), 0, false)
             .onKeyEvent("left"), 
             new MyWorld(new Random(0),
                 new PlayerFish(350, 400, -8, 2, 35, new FromFileImage("playerfish.png")), 
                 new ConsLoFish(new EnemyFish(40, 1, 5, 5, 10, 
                     new FromFileImage("enemyfish.png")), 
                     new ConsLoFish(new EnemyFish(200, 20, 15, 0, 15, 
                         new FromFileImage("enemyfish.png")), 
                         new ConsLoFish(new EnemyFish(50, 50, 2, 12, 35, 
                             new FromFileImage("enemyfish.png")), new MtLoFish()))), 0, false))
         
        && t.checkExpect(new MyWorld(new Random(0), 
            new PlayerFish(350, 400, -6, 2, 35, new FromFileImage("playerfish.png")), 
            new ConsLoFish(new EnemyFish(40, 1, 5, 5, 10, new FromFileImage("enemyfish.png")),
                new ConsLoFish(new EnemyFish(200, 20, 15, 0, 15, 
                    new FromFileImage("enemyfish.png")),
                    new ConsLoFish(new EnemyFish(50, 50, 2, 12, 35, 
                        new FromFileImage("enemyfish.png")), new MtLoFish()))), 0, false)
            .onKeyEvent("s"), 
            new MyWorld(new Random(0), 
                new PlayerFish(350, 400, -6, 2, 35, new FromFileImage("playerfish.png")), 
                new ConsLoFish(new EnemyFish(40, 1, 5, 5, 10, new FromFileImage("enemyfish.png")), 
                    new ConsLoFish(new EnemyFish(200, 20, 15, 0, 15, 
                        new FromFileImage("enemyfish.png")), 
                        new ConsLoFish(new EnemyFish(50, 50, 2, 12, 35, 
                            new FromFileImage("enemyfish.png")), new MtLoFish()))), 0, false));
  }
  
  
  /*
  boolean testOnTick(Tester t) {
    return t.checkExpect(this.w1.onTick(), this.w1) 
        && t.checkExpect(new MyWorld(new Random(0), 
      new PlayerFish(350, 400, -6, 2, 40, new FromFileImage("playerfish.png")), 
      new ConsLoFish(this.e1, new ConsLoFish(this.e2, 
          new ConsLoFish(this.e3, this.mt))), 0, false).onTick(), 
            new MyWorld(new Random(0), 
                new PlayerFish(350, 400, -6, 2, 40, new FromFileImage("playerfish.png")), 
                new ConsLoFish(this.e1, new ConsLoFish(this.e2, 
                    new ConsLoFish(this.e3, this.mt))), 0, true).endOfWorld("YOU WIN!"));
  }
  */
  
  boolean testOnTick(Tester t) {
    return t.checkExpect(new MyWorld(new Random(0), 
        new PlayerFish(650, 580, 5, -5, 10, new FromFileImage("playerfish.png")), 
        new MtLoFish(), 0, false).onTick(), new MyWorld(new Random(0), 
            new PlayerFish(650, 580, 5, -5, 10, new FromFileImage("playerfish.png")), 
            new MtLoFish(), 0, false).onTick()) 
        && t.checkExpect(new MyWorld(new Random(0), 
      new PlayerFish(350, 400, -6, 2, 40, new FromFileImage("playerfish.png")), 
      new ConsLoFish(new EnemyFish(40, 1, 5, 5, 10, new FromFileImage("enemyfish.png")), 
          new ConsLoFish(new EnemyFish(200, 20, 15, 0, 15, new FromFileImage("enemyfish.png")), 
          new ConsLoFish(new EnemyFish(50, 50, 2, 12, 35, new FromFileImage("enemyfish.png")), 
              new MtLoFish()))), 0, false).onTick(), 
            new MyWorld(new Random(0), 
                new PlayerFish(350, 400, -6, 2, 40, new FromFileImage("playerfish.png")), 
                new ConsLoFish(new EnemyFish(40, 1, 5, 5, 10, new FromFileImage("enemyfish.png")),
                    new ConsLoFish(new EnemyFish(200, 20, 15, 0, 15, 
                        new FromFileImage("enemyfish.png")), 
                        new ConsLoFish(new EnemyFish(50, 50, 2, 12, 35, 
                            new FromFileImage("enemyfish.png")), 
                            new MtLoFish()))), 0, true).endOfWorld("YOU WIN!"))
        && t.checkExpect(new MyWorld(new Random(0), 
      new PlayerFish(350, 400, -6, 2, 40, new FromFileImage("playerfish.png")), 
      new ConsLoFish(new EnemyFish(350, 400, 5, 5, 10, new FromFileImage("enemyfish.png")), 
          new MtLoFish()), 0, false).onTick(), 
            new MyWorld(new Random(0), 
                new PlayerFish(350, 400, -6, 2, 40, new FromFileImage("playerfish.png")), 
                new ConsLoFish(new EnemyFish(350, 400, 5, 5, 10, new FromFileImage("enemyfish.png")), 
                    new MtLoFish()), 0, true).endOfWorld("YOU GOT EATED!"));
  }
  
  
  
}
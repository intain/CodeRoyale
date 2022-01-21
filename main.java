import java.util.*;
import java.io.*;
import java.math.*;
import java.util.stream.*;

class Vector2 {
    public float x, y;

    public Vector2(float x, float y) {
        set(x, y);
    }

    public Vector2() {
        this(0, 0);
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public void offset(float x, float y) {
        this.x += x;
        this.y += y;
    }

    public void offset(Vector2 other) {
        offset(other.x, other.y);
    }

    public Vector2 add(Vector2 other) {
        return new Vector2(x + other.x, y + other.y);
    }

    public Vector2 substract(Vector2 other) {
        return new Vector2(x - other.x, y - other.y);
    }

    public Vector2 multiply(float factor) {
        return new Vector2(factor * x, factor * y);
    }

    public Vector2 divide(float factor) {
        return new Vector2(x / factor, y / factor);
    }

    public float magnitude() {
        return (float)Math.sqrt(x * x + y * y);
    }

    public Vector2 normalized() {
        return divide(magnitude());
    }

    public Vector2 opposite() {
        return new Vector2(-x, -y);
    }

    public String toString() {
        return (int)x + " " + (int)y;
    }
}

class Entity {
    public Vector2 position;
    public int owner;

    public Entity(int x, int y) {
        position = new Vector2(x, y);
    }

    public float distanceTo(Entity other) {
        return position.substract(other.position).magnitude();
    }

    public EntityDistance findClosest(List<? extends Entity> entities) {
        if(entities.isEmpty()) return null;
        Entity closest = entities.get(0);
        float dist = distanceTo(closest);

        for(int i = 1; i < entities.size(); i++) {
            float newDist = distanceTo(entities.get(i));
            if(newDist < dist) {
                closest = entities.get(i);
                dist = newDist;
            }

        }

        return new EntityDistance(closest, dist);
    }
}

class EntityDistance {
    public Entity entity;
    public float distance;

    public EntityDistance(Entity entity, float distance) {
        this.entity = entity;
        this.distance = distance;
    }
}

class BuildingSite extends Entity {
    public int id, radius, structureType, param1, param2;

    public BuildingSite(int id, int x, int y, int radius) {
        super(x, y);
        this.radius = radius;
        this.id = id;
    }

    public void update(int structureType, int owner, int param1, int param2) {
        this.structureType = structureType;
        this.owner = owner;
        this.param1 = param1;
        this.param2 = param2;
    }
}

class Building {
    public static final int TYPE_COUNT = 5;

    public static String getName(int id) {
        switch(id) {
            case 0: return "MINE";
            case 1: return "TOWER";
            case 2: return "BARRACKS-KNIGHT";
            case 3: return "BARRACKS-ARCHER";
            case 4: return "BARRACKS-GIANT";
        }

        return "";
    }

}

class Unit extends Entity {
    public static final int TYPE_COUNT = 3;

    public static final int[] COSTS = new int[] { 80, 100, 140 };
    public static final int[] GROUP_SIZE = new int[] { 4, 2, 1 };

    public int type, health;

    public Unit(int x, int y, int owner, int type, int health) {
        super(x, y);
        this.owner = owner;
        this.type = type;
        this.health = health;
    }

    public void update(int x, int y, int health) {
        position.set(x, y);
        this.health = health;
    }
}

class Queen extends Unit {
    public Queen(int owner) {
        super(0, 0, owner, -1, 100);
    }
}

class Competitor {
    public Queen queen;
    public List<Unit> units;
    public List<BuildingSite> ownedSites;

    public int[] unitCounts, buildingCounts;

    public Competitor(int id) {
        units = new ArrayList<Unit>();
        ownedSites = new ArrayList<BuildingSite>();
        queen = new Queen(id);

        unitCounts = new int[Unit.TYPE_COUNT];
        buildingCounts = new int[Building.TYPE_COUNT];
    }

    public void reset() {
        units.clear();
        ownedSites.clear();

        for(int i = 0; i < Unit.TYPE_COUNT; i++) {
            unitCounts[i] = 0;
        }

        for(int i = 0; i < Building.TYPE_COUNT; i++) {
            buildingCounts[i] = 0;
        }
    }

    public void addSite(BuildingSite site) {
        ownedSites.add(site);
        int id = site.structureType;
        if(id == 2) id += site.param2;
        buildingCounts[id]++;

        if(site.structureType == 2 && site.param1 > 0) {
            unitCounts[site.param2] += Unit.GROUP_SIZE[site.param2];
        }
    }

    public void addUnit(Unit unit) {
        units.add(unit);
        unitCounts[unit.type]++;
    }
}

class PlayerAI {
    static final int ENEMY_UNIT_MARGIN = 100;

    int gold, touchedSite, lastTouchedSite;
    Competitor self, enemy;
    BuildingSite[] buildingSites;
    
    int[] targetUnitCounts = new int[] { 15, 0, 0 };
    int[] targetBuildingCounts = new int[] { 4, 1, 1, 0, 0 };

    public PlayerAI(Competitor self, Competitor enemy, BuildingSite[] sites) {
        this.self = self;
        this.enemy = enemy;
        this.buildingSites = sites;
        this.touchedSite = lastTouchedSite = -1;
    }
    
    public void update(int gold, int touchedSite) {
        this.gold = gold;
        if(touchedSite != -1) {
            lastTouchedSite = this.touchedSite;
            this.touchedSite = touchedSite;
        }
    }

    int getNext(int[] target, int[] values) {
        int id = -1;
        int level = 0;
        int remainder = 0;

        for(int i = 0; i < target.length; i++) {
            if(target[i] != 0) {
                int newLevel = values[i] / target[i];
                int newRemainder = values[i] % target[i];

                if(id < 0 || newLevel < level || (newLevel == level && (newRemainder < remainder || (newRemainder == remainder && target[i] > target[id])))) {
                    id = i;
                    level = newLevel;
                    remainder = newRemainder; 
                }
            }
        }

        return id;
    }
    
    public String getNextQueenAction() {
        //if(enemy.unitCounts[0] == 0) return "WAIT";

        EntityDistance closestEnemy = self.queen.findClosest(enemy.units.stream().filter(n -> n.type == 0).collect(Collectors.toList()));

        if(closestEnemy != null && closestEnemy.distance < 400 && self.ownedSites.size() >= 5) {
            EntityDistance closestArcher = self.queen.findClosest(self.units.stream().filter(n -> n.type == 1).collect(Collectors.toList()));
            EntityDistance closestTower = self.queen.findClosest(self.ownedSites.stream().filter(n -> n.structureType == 1 && n.id != lastTouchedSite).collect(Collectors.toList()));
            EntityDistance closest = null;

            if(closestTower != null && closestArcher != null) {
                closest = closestTower;   
            } else if(closestTower == null) {
                closest = closestArcher;
            } else {
                closest = closestTower;
            }

            if(closest != null) {
                return "MOVE " + closest.entity.position.toString();
            }
        } 
        
        
        EntityDistance closest = self.queen.findClosest(Arrays.stream(buildingSites).filter(n -> n.owner == -1).collect(Collectors.toList()));
        if(closest != null) {
            BuildingSite target = (BuildingSite)closest.entity;
            int typeId = getNext(targetBuildingCounts, self.buildingCounts);
            return "BUILD " + target.id + " " + Building.getName(typeId);
        }     

        return "WAIT";

    }
    
    public String getNextTraining() {
        boolean canBuild = true;
        List<Integer> siteIds = new ArrayList<Integer>();

        while(canBuild) {
            canBuild = false;
            int id = getNext(targetUnitCounts, self.unitCounts);
            if(gold > Unit.COSTS[id]) {
                EntityDistance closest = enemy.queen.findClosest(self.ownedSites.stream().filter(n -> n.structureType == 2 && n.param2 == id && n.param1 == 0 && !siteIds.contains(n.id)).collect(Collectors.toList()));
                if(closest != null) {
                    siteIds.add(((BuildingSite)closest.entity).id);
                    gold -= Unit.COSTS[id];
                    canBuild = true;
                }
            }
        }

        String response = "TRAIN";
        for(Integer id : siteIds) {
            response += " " + id;
        }

        return response;
    }
}

class Field {
    public static final int WIDTH = 1920, HEIGHT = 1000;
}

class Player {

    public static void main(String args[]) {
        
        Competitor[] players = new Competitor[] { new Competitor(0),  new Competitor(1) };

        Scanner in = new Scanner(System.in);
        int numSites = in.nextInt();

        BuildingSite[] buildingSites = new BuildingSite[numSites];

        PlayerAI ai = new PlayerAI(players[0], players[1], buildingSites);

        for (int i = 0; i < numSites; i++) {
            int siteId = in.nextInt();
            int x = in.nextInt();
            int y = in.nextInt();
            int radius = in.nextInt();

            buildingSites[siteId] = new BuildingSite(siteId, x, y, radius);
        }

        // game loop
        while (true) {
            for(int i = 0; i < players.length; i++) {
                players[i].reset();
            }

            int gold = in.nextInt();
            int touchedSite = in.nextInt(); // -1 if none

            ai.update(gold, touchedSite);

            for (int i = 0; i < numSites; i++) {
                int siteId = in.nextInt();
                int ignore1 = in.nextInt(); // used in future leagues
                int ignore2 = in.nextInt(); // used in future leagues
                int structureType = in.nextInt(); // -1 = No structure, 2 = Barracks
                int owner = in.nextInt(); // -1 = No structure, 0 = Friendly, 1 = Enemy
                int param1 = in.nextInt();
                int param2 = in.nextInt();

                buildingSites[siteId].update(structureType, owner, param1, param2);
                if(owner >= 0) players[owner].addSite(buildingSites[siteId]);
            }

            int numUnits = in.nextInt();
            for (int i = 0; i < numUnits; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                int owner = in.nextInt();
                int unitType = in.nextInt(); // -1 = QUEEN, 0 = KNIGHT, 1 = ARCHER
                int health = in.nextInt();

                if(unitType == -1) {
                    players[owner].queen.update(x, y, health);
                } else {
                    players[owner].addUnit(new Unit(x, y, owner, unitType, health));
                }
            }

            // First line: A valid queen action
            // Second line: A set of training instructions
            System.out.println(ai.getNextQueenAction());
            System.out.println(ai.getNextTraining());
        }
    }
}
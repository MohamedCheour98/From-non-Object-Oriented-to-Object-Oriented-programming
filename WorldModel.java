import processing.core.PImage;

import java.util.*;

/*
WorldModel ideally keeps track of the actual size of our grid world and what is in that world
in terms of entities and background elements
 */

final class WorldModel
{
   private final int numRows;
   private final int numCols;
   private final Background background[][];
   private final Entity occupancy[][];
   private final Set<Entity> entities;

   public WorldModel(int numRows, int numCols, Background defaultBackground)
   {
      this.numRows = numRows;
      this.numCols = numCols;
      this.background = new Background[numRows][numCols];
      this.occupancy = new Entity[numRows][numCols];
      this.entities = new HashSet<>();

      for (int row = 0; row < numRows; row++)
      {
         Arrays.fill(this.background[row], defaultBackground);
      }
   }

   public int getNumRows() {
      return numRows;
   }

   public int getNumCols() {
      return numCols;
   }

   public Set<Entity> getEntities() {
      return entities;
   }

   private boolean withinBounds(Point pos)
   {
      return pos.y >= 0 && pos.y < this.numRows &&
              pos.x >= 0 && pos.x < this.numCols;
   }

   private Entity getOccupancyCell(Point pos)
   {
      return this.occupancy[pos.y][pos.x];
   }

   private Background getBackgroundCell(Point pos)
   {
      return this.background[pos.y][pos.x];
   }

   public boolean isOccupied(Point pos)
   {
      return this.withinBounds(pos) &&
              this.getOccupancyCell(pos) != null;
   }

   private static Entity createAtlantis(String id, Point position,
                                       List<PImage> images)
   {
      return new Entity(EntityKind.ATLANTIS, id, position, images,
              0, 0, 0, 0);
   }

   public Optional<Entity> getOccupant(Point pos)
   {
      if (this.isOccupied(pos))
      {
         return Optional.of(this.getOccupancyCell(pos));
      }
      else
      {
         return Optional.empty();
      }
   }

   public Optional<PImage> getBackgroundImage(Point pos)
   {
      if (this.withinBounds(pos))
      {
         return Optional.of(WorldView.getCurrentImage(this.getBackgroundCell(pos)));
      }
      else
      {
         return Optional.empty();
      }
   }

   private Entity createSgrass(String id, Point position, int actionPeriod,
                                     List<PImage> images)
   {
      return new Entity(EntityKind.SGRASS, id, position, images, 0, 0,
              actionPeriod, 0);
   }

   public static Entity createQuake(Point position, List<PImage> images)
   {
      return new Entity(EntityKind.QUAKE, Functions.QUAKE_ID, position, images,
              0, 0, Functions.QUAKE_ACTION_PERIOD, Functions.QUAKE_ANIMATION_PERIOD);
   }

   public static Entity createCrab(String id, Point position,
                                   int actionPeriod, int animationPeriod, List<PImage> images)
   {
      return new Entity(EntityKind.CRAB, id, position, images,
              0, 0, actionPeriod, animationPeriod);
   }

   public static Entity createFish(String id, Point position, int actionPeriod,
                                   List<PImage> images)
   {
      return new Entity(EntityKind.FISH, id, position, images, 0, 0,
              actionPeriod, 0);
   }

   private static Entity createObstacle(String id, Point position,
                                       List<PImage> images)
   {
      return new Entity(EntityKind.OBSTACLE, id, position, images,
              0, 0, 0, 0);
   }

   public static Entity createOctoNotFull(String id, int resourceLimit,
                                          Point position, int actionPeriod, int animationPeriod,
                                          List<PImage> images)
   {
      return new Entity(EntityKind.OCTO_NOT_FULL, id, position, images,
              resourceLimit, 0, actionPeriod, animationPeriod);
   }

   public static Entity createOctoFull(String id, int resourceLimit,
                                       Point position, int actionPeriod, int animationPeriod,
                                       List<PImage> images)
   {
      return new Entity(EntityKind.OCTO_FULL, id, position, images,
              resourceLimit, resourceLimit, actionPeriod, animationPeriod);
   }

   private void setBackgroundCell(Point pos,
                                        Background background)
   {
      this.background[pos.y][pos.x] = background;
   }

   private void setOccupancyCell(Point pos,
                                       Entity entity)
   {
      this.occupancy[pos.y][pos.x] = entity;
   }

   private void setBackground(Point pos,
                                    Background background)
   {
      if (this.withinBounds(pos))
      {
         this.setBackgroundCell(pos, background);
      }
   }

   private void removeEntityAt(Point pos)
   {
      if (this.withinBounds(pos)
              && this.getOccupancyCell(pos) != null)
      {
         Entity entity = this.getOccupancyCell(pos);

         /* this moves the entity just outside of the grid for
            debugging purposes */
            entity.position = new Point(-1, -1);
         this.entities.remove(entity);
         this.setOccupancyCell(pos, null);
      }
   }

   public void removeEntity(Entity entity)
   {
      this.removeEntityAt(entity.position);
   }

   public void moveEntity(Entity entity, Point pos)
   {
      Point oldPos = entity.position;
      if (this.withinBounds(pos) && !pos.equals(oldPos))
      {
         this.setOccupancyCell(oldPos, null);
         this.removeEntityAt(pos);
         this.setOccupancyCell(pos, entity);
         entity.position = pos;
      }
   }

   public  void addEntity(Entity entity)
   {
      if (this.withinBounds(entity.position))
      {
         this.setOccupancyCell(entity.position, entity);
         this.entities.add(entity);
      }
   }

   private static Optional<Entity> nearestEntity(List<Entity> entities,
                                                Point pos)
   {
      if (entities.isEmpty())
      {
         return Optional.empty();
      }
      else
      {
         Entity nearest = entities.get(0);
         int nearestDistance = nearest.position.distanceSquared(pos);

         for (Entity other : entities)
         {
            int otherDistance = other.position.distanceSquared(pos);

            if (otherDistance < nearestDistance)
            {
               nearest = other;
               nearestDistance = otherDistance;
            }
         }

         return Optional.of(nearest);
      }
   }

   public Optional<Entity> findNearest(Point pos,
                                              EntityKind kind)
   {
      List<Entity> ofType = new LinkedList<>();
      for (Entity entity : this.entities)
      {
         if (entity.getKind() == kind)
         {
            ofType.add(entity);
         }
      }

      return this.nearestEntity(ofType, pos);
   }

   private void tryAddEntity(Entity entity)
   {
      if (this.isOccupied(entity.position))
      {
         // arguably the wrong type of exception, but we are not
         // defining our own exceptions yet
         throw new IllegalArgumentException("position occupied");
      }

      this.addEntity(entity);
   }

   public Optional<Point> findOpenAround(Point pos)
   {
      for (int dy = -Functions.FISH_REACH; dy <= Functions.FISH_REACH; dy++)
      {
         for (int dx = -Functions.FISH_REACH; dx <= Functions.FISH_REACH; dx++)
         {
            Point newPt = new Point(pos.x + dx, pos.y + dy);
            if (this.withinBounds(newPt) &&
                    !this.isOccupied(newPt))
            {
               return Optional.of(newPt);
            }
         }
      }

      return Optional.empty();
   }

   public boolean parseSgrass(String [] properties,
                                     ImageStore imageStore)
   {
      if (properties.length == Functions.SGRASS_NUM_PROPERTIES)
      {
         Point pt = new Point(Integer.parseInt(properties[Functions.SGRASS_COL]),
                 Integer.parseInt(properties[Functions.SGRASS_ROW]));
         Entity entity = this.createSgrass(properties[Functions.SGRASS_ID],
                 pt,
                 Integer.parseInt(properties[Functions.SGRASS_ACTION_PERIOD]),
                 imageStore.getImageList(Functions.SGRASS_KEY));
         this.tryAddEntity(entity);
      }

      return properties.length == Functions.SGRASS_NUM_PROPERTIES;
   }

   public  boolean parseAtlantis(String [] properties,
                                       ImageStore imageStore)
   {
      if (properties.length == Functions.ATLANTIS_NUM_PROPERTIES)
      {
         Point pt = new Point(Integer.parseInt(properties[Functions.ATLANTIS_COL]),
                 Integer.parseInt(properties[Functions.ATLANTIS_ROW]));
         Entity entity = WorldModel.createAtlantis(properties[Functions.ATLANTIS_ID],
                 pt, imageStore.getImageList(Functions.ATLANTIS_KEY));
         this.tryAddEntity(entity);
      }

      return properties.length == Functions.ATLANTIS_NUM_PROPERTIES;
   }

   public  boolean parseFish(String [] properties,
                                   ImageStore imageStore)
   {
      if (properties.length == Functions.FISH_NUM_PROPERTIES)
      {
         Point pt = new Point(Integer.parseInt(properties[Functions.FISH_COL]),
                 Integer.parseInt(properties[Functions.FISH_ROW]));
         Entity entity = this.createFish(properties[Functions.FISH_ID],
                 pt, Integer.parseInt(properties[Functions.FISH_ACTION_PERIOD]),
                 imageStore.getImageList(Functions.FISH_KEY));
         this.tryAddEntity(entity);
      }

      return properties.length == Functions.FISH_NUM_PROPERTIES;
   }

   public boolean parseObstacle(String [] properties,
                                       ImageStore imageStore)
   {
      if (properties.length == Functions.OBSTACLE_NUM_PROPERTIES)
      {
         Point pt = new Point(
                 Integer.parseInt(properties[Functions.OBSTACLE_COL]),
                 Integer.parseInt(properties[Functions.OBSTACLE_ROW]));
         Entity entity = this.createObstacle(properties[Functions.OBSTACLE_ID],
                 pt, imageStore.getImageList(Functions.OBSTACLE_KEY));
         this.tryAddEntity(entity);
      }

      return properties.length == Functions.OBSTACLE_NUM_PROPERTIES;
   }

   public boolean parseBackground(String [] properties, ImageStore imageStore)
   {
      if (properties.length == Functions.BGND_NUM_PROPERTIES)
      {
         Point pt = new Point(Integer.parseInt(properties[Functions.BGND_COL]),
                 Integer.parseInt(properties[Functions.BGND_ROW]));
         String id = properties[Functions.BGND_ID];
         this.setBackground(pt,
                 new Background(id, imageStore.getImageList(id)));
      }

      return properties.length == Functions.BGND_NUM_PROPERTIES;
   }

   public boolean parseOcto(String [] properties,
                                   ImageStore imageStore)
   {
      if (properties.length == Functions.OCTO_NUM_PROPERTIES)
      {
         Point pt = new Point(Integer.parseInt(properties[Functions.OCTO_COL]),
                 Integer.parseInt(properties[Functions.OCTO_ROW]));
         Entity entity = this.createOctoNotFull(properties[Functions.OCTO_ID],
                 Integer.parseInt(properties[Functions.OCTO_LIMIT]),
                 pt,
                 Integer.parseInt(properties[Functions.OCTO_ACTION_PERIOD]),
                 Integer.parseInt(properties[Functions.OCTO_ANIMATION_PERIOD]),
                 imageStore.getImageList(Functions.OCTO_KEY));
         this.tryAddEntity(entity);
      }

      return properties.length == Functions.OCTO_NUM_PROPERTIES;
   }




}

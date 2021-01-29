/*
Action: ideally what our various entities might do in our virtual world
 */

import java.util.Optional;

final class Action
{
   private final ActionKind kind;
   private final Entity entity;
   private final WorldModel world;
   private final ImageStore imageStore;
   private final int repeatCount;

   public Action(ActionKind kind, Entity entity, WorldModel world,
      ImageStore imageStore, int repeatCount)
   {
      this.kind = kind;
      this.entity = entity;
      this.world = world;
      this.imageStore = imageStore;
      this.repeatCount = repeatCount;
   }

   public ActionKind getKind() {
      return kind;
   }

   public Entity getEntity() {
      return entity;
   }

   public WorldModel getWorld() {
      return world;
   }

   public ImageStore getImageStore() {
      return imageStore;
   }

   public int getRepeatCount() {
      return repeatCount;
   }

   private static Point nextPositionCrab(Entity entity, WorldModel world,
                                        Point destPos)
   {
      int horiz = Integer.signum(destPos.x - entity.position.x);
      Point newPos = new Point(entity.position.x + horiz,
              entity.position.y);

      Optional<Entity> occupant = world.getOccupant(newPos);

      if (horiz == 0 ||
              (occupant.isPresent() && !(occupant.get().getKind() == EntityKind.FISH)))
      {
         int vert = Integer.signum(destPos.y - entity.position.y);
         newPos = new Point(entity.position.x, entity.position.y + vert);
         occupant = world.getOccupant(newPos);

         if (vert == 0 ||
                 (occupant.isPresent() && !(occupant.get().getKind() == EntityKind.FISH)))
         {
            newPos = entity.position;
         }
      }

      return newPos;
   }

   private Point nextPositionOcto(Entity entity, WorldModel world,
                                        Point destPos)
   {
      int horiz = Integer.signum(destPos.x - entity.position.x);
      Point newPos = new Point(entity.position.x + horiz,
              entity.position.y);

      if (horiz == 0 || world.isOccupied(newPos))
      {
         int vert = Integer.signum(destPos.y - entity.position.y);
         newPos = new Point(entity.position.x,
                 entity.position.y + vert);

         if (vert == 0 || world.isOccupied(newPos))
         {
            newPos = entity.position;
         }
      }

      return newPos;
   }

   public static boolean moveToCrab(Entity crab, WorldModel world,
                                    Entity target, EventScheduler scheduler)
   {
      if (crab.position.adjacent(target.position))
      {
         world.removeEntity(target);
         scheduler.unscheduleAllEvents(target);
         return true;
      }
      else
      {
         Point nextPos = Action.nextPositionCrab(crab, world, target.position);

         if (!crab.position.equals(nextPos))
         {
            Optional<Entity> occupant = world.getOccupant(nextPos);
            if (occupant.isPresent())
            {
               scheduler.unscheduleAllEvents(occupant.get());
            }

            world.moveEntity(crab, nextPos);
         }
         return false;
      }
   }

   public static boolean moveToFull(Entity octo, WorldModel world,
                                    Entity target, EventScheduler scheduler)
   {
      if (octo.position.adjacent(target.position))
      {
         return true;
      }
      else
      {
         Point nextPos = Action.nextPositionCrab(octo, world, target.position);

         if (!octo.position.equals(nextPos))
         {
            Optional<Entity> occupant = world.getOccupant(nextPos);
            if (occupant.isPresent())
            {
               scheduler.unscheduleAllEvents(occupant.get());
            }

            world.moveEntity(octo, nextPos);
         }
         return false;
      }
   }

   public static boolean moveToNotFull(Entity octo, WorldModel world,
                                       Entity target, EventScheduler scheduler)
   {
      if (octo.position.adjacent(target.position))
      {
         octo.getResourceCount();
         world.removeEntity(target);
         scheduler.unscheduleAllEvents(target);

         return true;
      }
      else
      {
         Point nextPos = Action.nextPositionCrab(octo, world, target.position);

         if (!octo.position.equals(nextPos))
         {
            Optional<Entity> occupant = world.getOccupant(nextPos);
            if (occupant.isPresent())
            {
               scheduler.unscheduleAllEvents(occupant.get());
            }

            world.moveEntity(octo, nextPos);
         }
         return false;
      }
   }



}

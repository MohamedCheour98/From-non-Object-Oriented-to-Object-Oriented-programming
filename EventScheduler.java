import java.util.*;
import java.util.function.Function;

/*
EventScheduler: ideally our way of controlling what happens in our virtual world
 */

final class EventScheduler
{
   private final PriorityQueue<Event> eventQueue;
   private final Map<Entity, List<Event>> pendingEvents;
   private final double timeScale;

   public EventScheduler(double timeScale)
   {
      this.eventQueue = new PriorityQueue<>(new EventComparator());
      this.pendingEvents = new HashMap<>();
      this.timeScale = timeScale;
   }

   private void scheduleEvent(
                                    Entity entity, Action action, long afterPeriod)
   {
      long time = System.currentTimeMillis() +
              (long)(afterPeriod * this.timeScale);
      Event event = new Event(action, time, entity);

      this.eventQueue.add(event);

      // update list of pending events for the given entity
      List<Event> pending = this.pendingEvents.getOrDefault(entity,
              new LinkedList<>());
      pending.add(event);
      this.pendingEvents.put(entity, pending);
   }

   private void removePendingEvent(
                                         Event event)
   {
      List<Event> pending = this.pendingEvents.get(event.getEntity());

      if (pending != null)
      {
         pending.remove(event);
      }
   }

   private void executeAnimationAction(Action action)
   {
      action.getEntity() .nextImage();

      if (action.getRepeatCount() != 1)
      {
         this.scheduleEvent(action.getEntity() ,
                 action.getEntity() .createAnimationAction(
                         Math.max(action.getRepeatCount() - 1, 0)),
                 action.getEntity() .getAnimationPeriod());
      }
   }

   public void unscheduleAllEvents(
                                          Entity entity)
   {
      List<Event> pending = this.pendingEvents.remove(entity);

      if (pending != null)
      {
         for (Event event : pending)
         {
            this.eventQueue.remove(event);
         }
      }
   }

   private  void executeAtlantisActivity(Entity entity, WorldModel world,
                                              ImageStore imageStore)
   {
      this.unscheduleAllEvents(entity);
      world.removeEntity(entity);
   }

   public void scheduleActions(Entity entity,
                                      WorldModel world, ImageStore imageStore)
   {
      switch (entity.getKind())
      {
         case OCTO_FULL:
            this.scheduleEvent(entity,
                    entity.createActivityAction(world, imageStore),
                    entity.getActionPeriod());
            this.scheduleEvent(entity, entity.createAnimationAction(0),
                    entity.getAnimationPeriod());
            break;

         case OCTO_NOT_FULL:
            this.scheduleEvent(entity,
                    entity.createActivityAction(world, imageStore),
                    entity.getActionPeriod());
            this.scheduleEvent(entity,
                    entity.createAnimationAction(0), entity.getAnimationPeriod());
            break;

         case FISH:
            this.scheduleEvent(entity,
                    entity.createActivityAction(world, imageStore),
                    entity.getActionPeriod());
            break;

         case CRAB:
            this.scheduleEvent(entity,
                    entity.createActivityAction(world, imageStore),
                    entity.getActionPeriod());
            this.scheduleEvent(entity,
                    entity.createAnimationAction(0), entity.getAnimationPeriod());
            break;

         case QUAKE:
            this.scheduleEvent(entity,
                    entity.createActivityAction(world, imageStore),
                    entity.getActionPeriod());
            this.scheduleEvent(entity,
                    entity.createAnimationAction(Functions.QUAKE_ANIMATION_REPEAT_COUNT),
                    entity.getAnimationPeriod());
            break;

         case SGRASS:
            this.scheduleEvent(entity,
                    entity.createActivityAction(world, imageStore),
                    entity.getActionPeriod());
            break;
         case ATLANTIS:
            this.scheduleEvent(entity,
                    entity.createAnimationAction(Functions.ATLANTIS_ANIMATION_REPEAT_COUNT),
                    entity.getAnimationPeriod());
            break;

         default:
      }
   }

   private void executeSgrassActivity(Entity entity, WorldModel world,
                                            ImageStore imageStore)
   {
      Optional<Point> openPt = world.findOpenAround(entity.position);

      if (openPt.isPresent())
      {
         Entity fish = world.createFish(Functions.FISH_ID_PREFIX + entity.getId(),
                 openPt.get(), Functions.FISH_CORRUPT_MIN +
                         Functions.rand.nextInt(Functions.FISH_CORRUPT_MAX - Functions.FISH_CORRUPT_MIN),
                 imageStore.getImageList(Functions.FISH_KEY));
         world.addEntity(fish);
         this.scheduleActions(fish, world, imageStore);
      }

      this.scheduleEvent(entity,
              entity.createActivityAction(world, imageStore),
              entity.getActionPeriod());
   }

   private void executeQuakeActivity(Entity entity, WorldModel world,
                                           ImageStore imageStore)
   {
      this.unscheduleAllEvents(entity);
      world.removeEntity(entity);
   }

   private void executeCrabActivity(Entity entity, WorldModel world,
                                          ImageStore imageStore)
   {
      Optional<Entity> crabTarget = world.findNearest(
              entity.position, EntityKind.SGRASS);
      long nextPeriod = entity.getActionPeriod();

      if (crabTarget.isPresent())
      {
         Point tgtPos = crabTarget.get().position;

         if (Action.moveToCrab(entity, world, crabTarget.get(), this))
         {
            Entity quake = world.createQuake(tgtPos,
                    imageStore.getImageList(Functions.QUAKE_KEY));

            world.addEntity(quake);
            nextPeriod += entity.getActionPeriod();
            this.scheduleActions(quake, world, imageStore);
         }
      }

      this.scheduleEvent(entity,
              entity.createActivityAction(world, imageStore),
              nextPeriod);
   }

   private void executeFishActivity(Entity entity, WorldModel world,
                                          ImageStore imageStore)
   {
      Point pos = entity.position;  // store current position before removing

      world.removeEntity(entity);
      this.unscheduleAllEvents(entity);

      Entity crab = world.createCrab(entity.getId() + Functions.CRAB_ID_SUFFIX,
              pos, entity.getActionPeriod() / Functions.CRAB_PERIOD_SCALE,
              Functions.CRAB_ANIMATION_MIN +
                      Functions.rand.nextInt(Functions.CRAB_ANIMATION_MAX - Functions.CRAB_ANIMATION_MIN),
              imageStore.getImageList(Functions.CRAB_KEY));

      world.addEntity(crab);
      this.scheduleActions(crab, world, imageStore);
   }


   private void executeOctoNotFullActivity(Entity entity,
                                                 WorldModel world, ImageStore imageStore)
   {
      Optional<Entity> notFullTarget = world.findNearest(entity.position,
              EntityKind.FISH);

      if (!notFullTarget.isPresent() ||
              !Action.moveToNotFull(entity, world, notFullTarget.get(), this) ||
              !entity.transformNotFull(world, this, imageStore))
      {
         this.scheduleEvent(entity,
                 entity.createActivityAction(world, imageStore),
                 entity.getActionPeriod());
      }
   }

   private void executeOctoFullActivity(Entity entity, WorldModel world,
                                              ImageStore imageStore)
   {
      Optional<Entity> fullTarget = world.findNearest(entity.position,
              EntityKind.ATLANTIS);

      if (fullTarget.isPresent() &&
              Action.moveToFull(entity, world, fullTarget.get(), this))
      {
         //at atlantis trigger animation
         this.scheduleActions(fullTarget.get(), world, imageStore);

         //transform to unfull
         entity.transformFull(world, this, imageStore);
      }
      else
      {
         this.scheduleEvent(entity,
                 entity.createActivityAction(world, imageStore),
                 entity.getActionPeriod());
      }
   }

   private void executeActivityAction(Action action)
   {
      switch (action.getEntity().getKind())
      {
         case OCTO_FULL:
            this.executeOctoFullActivity(action.getEntity() , action.getWorld(),
                    action.getImageStore());
            break;

         case OCTO_NOT_FULL:
            this.executeOctoNotFullActivity(action.getEntity() , action.getWorld(),
                    action.getImageStore());
            break;

         case FISH:
            this.executeFishActivity(action.getEntity() , action.getWorld(), action.getImageStore());
            break;

         case CRAB:
            this.executeCrabActivity(action.getEntity() , action.getWorld(),
                    action.getImageStore());
            break;

         case QUAKE:
            this.executeQuakeActivity(action.getEntity() , action.getWorld(), action.getImageStore());
            break;

         case SGRASS:
            this.executeSgrassActivity(action.getEntity() , action.getWorld(), action.getImageStore());
            break;

         case ATLANTIS:
            this.executeAtlantisActivity(action.getEntity() , action.getWorld(), action.getImageStore());
            break;

         default:
            throw new UnsupportedOperationException(
                    String.format("executeActivityAction not supported for %s",
                            action.getEntity().getKind()));
      }
   }


   private void executeAction(Action action)
   {
      switch (action.getKind())
      {
         case ACTIVITY:
            this.executeActivityAction(action);
            break;

         case ANIMATION:
            this.executeAnimationAction(action);
            break;
      }
   }

   public  void updateOnTime(long time)
   {
      while (!this.eventQueue.isEmpty() &&
              this.eventQueue.peek().getTime() < time)
      {
         Event next = this.eventQueue.poll();

         this.removePendingEvent(next);

         this.executeAction(next.getAction());
      }
   }




}

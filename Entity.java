import java.util.List;
import processing.core.PImage;

/*
Entity ideally would include functions for how all the entities in our virtual world might act...
 */


final class Entity
{
   private final EntityKind kind;
   private final String id;
   public Point position;
   private final List<PImage> images;
   private int imageIndex;
   private final int resourceLimit;
   private int resourceCount;
   private final int actionPeriod;
   private final int animationPeriod;

   public Entity(EntityKind kind, String id, Point position,
      List<PImage> images, int resourceLimit, int resourceCount,
      int actionPeriod, int animationPeriod)
   {
      this.kind = kind;
      this.id = id;
      this.position = position;
      this.images = images;
      this.imageIndex = 0;
      this.resourceLimit = resourceLimit;
      this.resourceCount = resourceCount;
      this.actionPeriod = actionPeriod;
      this.animationPeriod = animationPeriod;
   }

   public EntityKind getKind() {
      return kind;
   }

   public String getId() {
      return id;
   }

   public List<PImage> getImages() {
      return images;
   }

   public int getImageIndex() {
      return imageIndex;
   }

   public int getResourceCount() {
      return resourceCount++;
   }

   public int getActionPeriod() {
      return actionPeriod;
   }

   public int getAnimationPeriod()
   {
      switch (this.kind)
      {
         case OCTO_FULL:
         case OCTO_NOT_FULL:
         case CRAB:
         case QUAKE:
         case ATLANTIS:
            return this.animationPeriod;
         default:
            throw new UnsupportedOperationException(
                    String.format("getAnimationPeriod not supported for %s",
                            this.kind));
      }
   }

   public void nextImage()
   {
      this.imageIndex = (this.imageIndex + 1) % this.images.size();
   }

   public Action createActivityAction(WorldModel world,
                                             ImageStore imageStore)
   {
      return new Action(ActionKind.ACTIVITY, this, world, imageStore, 0);
   }

   public Action createAnimationAction(int repeatCount)
   {
      return new Action(ActionKind.ANIMATION, this, null, null, repeatCount);
   }

   public void transformFull(WorldModel world,
                                    EventScheduler scheduler, ImageStore imageStore)
   {
      Entity octo = world.createOctoNotFull(this.id, this.resourceLimit,
              this.position, this.actionPeriod, this.animationPeriod,
              this.images);

      world.removeEntity(this);
      scheduler.unscheduleAllEvents(this);

      world.addEntity(octo);
      scheduler.scheduleActions(octo, world, imageStore);
   }

   public  boolean transformNotFull(WorldModel world,
                                          EventScheduler scheduler, ImageStore imageStore)
   {
      if (this.resourceCount >= this.resourceLimit)
      {
         Entity octo = world.createOctoFull(this.id, this.resourceLimit,
                 this.position, this.actionPeriod, this.animationPeriod,
                 this.images);

         world.removeEntity(this);
         scheduler.unscheduleAllEvents(this);

         world.addEntity(octo);
         scheduler.scheduleActions(octo, world, imageStore);

         return true;
      }

      return false;
   }





}

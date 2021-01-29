import java.util.*;

import processing.core.PApplet;
import processing.core.PImage;

/*
ImageStore: to ideally keep track of the images used in our virtual world
 */

final class ImageStore
{
   private final Map<String, List<PImage>> images;
   private final List<PImage> defaultImages;

   public ImageStore(PImage defaultImage)
   {
      this.images = new HashMap<>();
      defaultImages = new LinkedList<>();
      defaultImages.add(defaultImage);
   }

   public List<PImage> getImageList(String key)
   {
      return this.images.getOrDefault(key, this.defaultImages);
   }

   private static List<PImage> getImages(Map<String, List<PImage>> images,
                                        String key)
   {
      List<PImage> imgs = images.get(key);
      if (imgs == null)
      {
         imgs = new LinkedList<>();
         images.put(key, imgs);
      }
      return imgs;
   }

   private void processImageLine(Map<String, List<PImage>> images,
                                       String line, PApplet screen)
   {
      String[] attrs = line.split("\\s");
      if (attrs.length >= 2)
      {
         String key = attrs[0];
         PImage img = screen.loadImage(attrs[1]);
         if (img != null && img.width != -1)
         {
            List<PImage> imgs = ImageStore.getImages(images, key);
            imgs.add(img);

            if (attrs.length >= Functions.KEYED_IMAGE_MIN)
            {
               int r = Integer.parseInt(attrs[Functions.getKeyedRedIdx()]);
               int g = Integer.parseInt(attrs[Functions.getKeyedGreenIdx()]);
               int b = Integer.parseInt(attrs[Functions.getKeyedBlueIdx()]);
               Functions.setAlpha(img, screen.color(r, g, b), 0);
            }
         }
      }
   }

   public  void loadImages(Scanner in,
                                 PApplet screen)
   {
      int lineNumber = 0;
      while (in.hasNextLine())
      {
         try
         {
            this.processImageLine(this.images, in.nextLine(), screen);
         }
         catch (NumberFormatException e)
         {
            System.out.println(String.format("Image format error on line %d",
                    lineNumber));
         }
         lineNumber++;
      }
   }
}

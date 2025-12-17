package app.organicmaps.sdk.util;

import android.content.Context;
import android.content.res.AssetManager;
import androidx.annotation.NonNull;
import app.organicmaps.sdk.util.log.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class to copy map files from assets to storage directory.
 * This allows bundling maps with the app to avoid downloading them.
 */
public class AssetMapsCopier
{
  private static final String TAG = AssetMapsCopier.class.getSimpleName();
  private static final String MAPS_ASSET_PATH = ""; // Root of assets folder
  private static final String MAP_FILE_EXTENSION = ".mwm";
  private static final int BUFFER_SIZE = 8192;

  /**
   * Copies all .mwm map files from assets to the specified storage path.
   * Only copies files that don't already exist in the destination.
   *
   * @param context Application context
   * @param destPath Destination directory path where maps should be copied
   */
  public static void copyMapsFromAssets(@NonNull Context context, @NonNull String destPath)
  {
    try
    {
      final AssetManager assetManager = context.getAssets();
      final File destDir = new File(destPath);

      // Ensure destination directory exists
      if (!destDir.exists() && !destDir.mkdirs())
      {
        Logger.e(TAG, "Failed to create destination directory: " + destPath);
        return;
      }

      // List all assets and filter for .mwm files
      String[] assetFiles = assetManager.list(MAPS_ASSET_PATH);
      if (assetFiles == null || assetFiles.length == 0)
      {
        Logger.i(TAG, "No assets found to copy");
        return;
      }

      int copiedCount = 0;
      int skippedCount = 0;

      for (String assetFile : assetFiles)
      {
        if (!assetFile.endsWith(MAP_FILE_EXTENSION))
        {
          continue;
        }

        final File destFile = new File(destDir, assetFile);

        // Skip if file already exists
        if (destFile.exists())
        {
          Logger.i(TAG, "Map file already exists, skipping: " + assetFile);
          skippedCount++;
          continue;
        }

        Logger.i(TAG, "Copying map file from assets: " + assetFile);

        try
        {
          copyAssetFile(assetManager, assetFile, destFile);
          copiedCount++;
          Logger.i(TAG, "Successfully copied: " + assetFile + " (" + destFile.length() + " bytes)");
        }
        catch (IOException e)
        {
          Logger.e(TAG, "Failed to copy " + assetFile, e);
        }
      }

      Logger.i(TAG, "Asset maps copy complete. Copied: " + copiedCount + ", Skipped: " + skippedCount);
    }
    catch (IOException e)
    {
      Logger.e(TAG, "Error accessing assets", e);
    }
  }

  /**
   * Copies a single file from assets to the filesystem.
   *
   * @param assetManager Asset manager to read from
   * @param assetPath Path to the asset file
   * @param destFile Destination file
   * @throws IOException if copy fails
   */
  private static void copyAssetFile(@NonNull AssetManager assetManager, @NonNull String assetPath,
                                    @NonNull File destFile) throws IOException
  {
    InputStream in = null;
    OutputStream out = null;

    try
    {
      in = assetManager.open(assetPath);
      out = new FileOutputStream(destFile);

      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1)
      {
        out.write(buffer, 0, bytesRead);
      }

      out.flush();
    }
    finally
    {
      if (in != null)
      {
        try { in.close(); } catch (IOException e) { /* ignore */ }
      }
      if (out != null)
      {
        try { out.close(); } catch (IOException e) { /* ignore */ }
      }
    }
  }
}

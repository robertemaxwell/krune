import { 
  Scene, 
  SceneLoader, 
  AssetsManager, 
  MeshAssetTask, 
  TextureAssetTask,
  Sound,
  Texture
} from "@babylonjs/core";

// Enum for asset types
export enum AssetType {
  TEXTURE,
  MODEL,
  SOUND
}

// Asset manager class for handling game assets
export class GameAssetManager {
  private scene: Scene;
  private assetsManager: AssetsManager;
  private textures: Map<string, Texture> = new Map();
  private models: Map<string, any> = new Map();
  private sounds: Map<string, Sound> = new Map();
  private loadingPromise: Promise<void> | null = null;
  private progressCallback: ((progress: number) => void) | null = null;
  private totalTaskCount: number = 0;
  private completedTaskCount: number = 0;
  private failedTaskCount: number = 0;

  constructor(scene: Scene) {
    this.scene = scene;
    this.assetsManager = new AssetsManager(scene);
    
    // Configure asset manager
    this.assetsManager.useDefaultLoadingScreen = false;
    
    // Custom progress tracking that accounts for failed tasks
    this.assetsManager.onTaskSuccess = (task) => {
      this.completedTaskCount++;
      this.updateProgress();
    };
    
    this.assetsManager.onTaskError = (task) => {
      console.warn(`Asset failed to load: ${task.name}`, task.errorObject);
      this.failedTaskCount++;
      this.updateProgress();
      
      // Don't let failures stop the entire loading process
      task.onError = () => {}; // Override default error handling
      if (task.onSuccess) {
        task.onSuccess(task); // Trigger success callback even on failure to keep processing
      }
    };
  }
  
  // Update progress considering both completed and failed tasks
  private updateProgress(): void {
    const totalProcessed = this.completedTaskCount + this.failedTaskCount;
    const progress = (totalProcessed / this.totalTaskCount) * 100;
    
    if (this.progressCallback) {
      this.progressCallback(progress);
    }
  }

  // Set a callback to monitor loading progress
  public setProgressCallback(callback: (progress: number) => void): void {
    this.progressCallback = callback;
  }

  // Add a texture to be loaded
  public addTexture(name: string, url: string): void {
    const task = this.assetsManager.addTextureTask(name, url);
    this.totalTaskCount++;
    
    task.onSuccess = (task) => {
      this.textures.set(name, task.texture);
    };
  }

  // Add a model to be loaded
  public addModel(name: string, rootUrl: string, filename: string): void {
    const task = this.assetsManager.addMeshTask(name, "", rootUrl, filename);
    this.totalTaskCount++;
    
    task.onSuccess = (task) => {
      this.models.set(name, {
        meshes: task.loadedMeshes,
        particleSystems: task.loadedParticleSystems,
        skeletons: task.loadedSkeletons
      });
    };
  }

  // Add a sound to be loaded
  public addSound(name: string, url: string, streaming: boolean = false): void {
    const task = this.assetsManager.addBinaryFileTask(name, url);
    this.totalTaskCount++;
    
    task.onSuccess = (task) => {
      try {
        const sound = new Sound(name, task.data, this.scene, null, {
          streaming: streaming
        });
        this.sounds.set(name, sound);
      } catch (error) {
        console.warn(`Failed to create sound from binary data: ${name}`, error);
      }
    };
  }

  // Get a loaded texture by name
  public getTexture(name: string): Texture | undefined {
    return this.textures.get(name);
  }

  // Get a loaded model by name
  public getModel(name: string): any | undefined {
    return this.models.get(name);
  }

  // Get a loaded sound by name
  public getSound(name: string): Sound | undefined {
    return this.sounds.get(name);
  }

  // Reset counters and prepare for loading
  private resetCounters(): void {
    this.completedTaskCount = 0;
    this.failedTaskCount = 0;
  }

  // Load all registered assets and return a promise
  public loadAssets(): Promise<void> {
    this.resetCounters();
    
    if (!this.loadingPromise) {
      this.loadingPromise = new Promise<void>((resolve) => {
        // Always resolve, even if some assets failed to load
        this.assetsManager.onFinish = () => {
          console.log(`Asset loading completed. Success: ${this.completedTaskCount}, Failed: ${this.failedTaskCount}`);
          resolve();
        };
        this.assetsManager.load();
      });
    }
    return this.loadingPromise;
  }

  // Clone a model instance for use in the scene
  public instantiateModel(name: string, newName: string): any | null {
    const model = this.models.get(name);
    if (!model) {
      console.warn(`Model ${name} not found`);
      return null;
    }

    // Safety check for meshes
    if (!model.meshes || model.meshes.length === 0) {
      console.warn(`Model ${name} has no meshes`);
      return null;
    }

    const rootMesh = model.meshes[0];
    if (rootMesh) {
      try {
        // Clone the entire model hierarchy
        const clone = rootMesh.clone(newName, null, true);
        return clone;
      } catch (error) {
        console.error(`Failed to clone model ${name}`, error);
        return null;
      }
    }

    return null;
  }

  // Check if a specific asset is loaded
  public isAssetLoaded(name: string, type: AssetType): boolean {
    switch (type) {
      case AssetType.TEXTURE:
        return this.textures.has(name);
      case AssetType.MODEL:
        return this.models.has(name);
      case AssetType.SOUND:
        return this.sounds.has(name);
      default:
        return false;
    }
  }

  // Clear all loaded assets
  public dispose(): void {
    // Dispose textures
    this.textures.forEach((texture) => {
      if (texture && texture.dispose) {
        texture.dispose();
      }
    });
    this.textures.clear();

    // Dispose models
    this.models.forEach((model) => {
      if (model && model.meshes) {
        model.meshes.forEach((mesh: any) => {
          if (mesh && mesh.dispose) {
            mesh.dispose();
          }
        });
      }
    });
    this.models.clear();

    // Dispose sounds
    this.sounds.forEach((sound) => {
      if (sound && sound.dispose) {
        sound.dispose();
      }
    });
    this.sounds.clear();
  }
} 
import fs from "fs";
import path from "path";

export function saveMetadataToFile(metadata: object, filePath: string): void {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, JSON.stringify(metadata, null, 2), "utf-8");
  console.log(`${filePath} file created successfully`);
}

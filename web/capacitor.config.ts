import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.anganwadi.parentconnect",
  appName: "Anganwadi Connect",
  webDir: "out",
  server: {
    androidScheme: "https"
  }
};

export default config;

import { authApi } from "../../users/authApi";
import { devices } from "../../../devices/devices";
import { defineAction } from "./registry";
import { msg } from "./types";

defineAction({
  id: "OPEN_DRAWER",
  async run() {
    await devices.openDrawer({});
    return [msg("info", "Drawer closed")];
  },
});

defineAction({
  id: "SIGN_OUT",
  needs: { day: false },
  async run() {
    await authApi.logout().catch(() => undefined);
    return [{ type: "signOut" }];
  },
});

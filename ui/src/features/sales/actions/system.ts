import { authApi } from "../../users/authApi";
import { defineAction } from "./registry";
import { msg } from "./types";

defineAction({
  id: "OPEN_DRAWER",
  run: () => [msg("info", "No cash drawer connected yet.")],
});

defineAction({
  id: "SIGN_OUT",
  needs: { day: false },
  async run() {
    await authApi.logout().catch(() => undefined);
    return [{ type: "signOut" }];
  },
});

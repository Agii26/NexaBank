import api from "./axios";
export const getUsers    = (page=0, size=20) => api.get("/admin/users",    { params: { page, size } });
export const getAccounts = (page=0, size=20) => api.get("/admin/accounts", { params: { page, size } });
export const freezeAccount   = (id) => api.patch(`/admin/accounts/${id}/freeze`);
export const unfreezeAccount = (id) => api.patch(`/admin/accounts/${id}/unfreeze`);

import api from "./axios";
export const createAccount  = (data) => api.post("/accounts", data);
export const getMyAccounts  = ()     => api.get("/accounts");
export const getAccountById = (id)   => api.get(`/accounts/${id}`);

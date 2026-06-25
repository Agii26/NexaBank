import api from "./axios";
export const deposit    = (data)                      => api.post("/transactions/deposit", data);
export const withdraw   = (data)                      => api.post("/transactions/withdraw", data);
export const transfer   = (data)                      => api.post("/transactions/transfer", data);
export const getHistory = (accountId, page=0, size=10) =>
  api.get(`/transactions/${accountId}/history`, { params: { page, size } });

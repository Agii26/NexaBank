import api from "./axios";

export const downloadStatement = async (accountId, from, to) => {
  const response = await api.get(`/statements/${accountId}`, {
    params: { from, to },
    responseType: "blob",
  });
  const url  = window.URL.createObjectURL(new Blob([response.data], { type: "application/pdf" }));
  const link = document.createElement("a");
  link.href     = url;
  link.download = `statement-${accountId}-${from}-to-${to}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

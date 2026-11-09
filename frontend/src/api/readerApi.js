import axiosClient from './axiosClient.js'

// The gateway works out who the reader is from the token: nothing about the reader is sent from here.
const readerApi = {
  getMyLoans: () => axiosClient.get('/me/loans'),
  getMyFines: () => axiosClient.get('/me/fines'),
}

export default readerApi

package lib

import com.securemessaging.Message
import com.securemessaging.SecureMessenger
import com.securemessaging.sm.Credentials
import com.securemessaging.sm.enums.MessageBoxType
import com.securemessaging.sm.search.SearchMessagesFilter
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class Executor {

    companion object {

        fun execute(configuration: Configuration):SearchResults{
            println("Executor Now Running")

            val serviceCode = configuration.serviceCode
            val username = configuration.username
            val password = configuration.password
            val query = configuration.query
            val useCache = configuration.useCache
            val cacheResults = configuration.cacheResults

            val absoluteSearch = configuration.absoluteSearch
            val searchBody = configuration.searchBody
            val searchSubject = configuration.searchSubject
            val searchRecipients = configuration.searchRecipients
            val caseSensitive = configuration.caseSensitive

            Searcher.SearchSettings.absoluteSearch = absoluteSearch
            Searcher.SearchSettings.searchBody = searchBody
            Searcher.SearchSettings.searchSubject = searchSubject
            Searcher.SearchSettings.searchRecipients = searchRecipients
            Searcher.SearchSettings.caseSensitive = caseSensitive

            val inboxList = ArrayList<Message>()
            val sentList = ArrayList<Message>()
            val draftList = ArrayList<Message>()
            val trashList = ArrayList<Message>()

            if(useCache){
                val localStorage = CacheManager().getCachedData(serviceCode)
                if(localStorage == null){
                    println("Caching Data Could Not Be Retrieved. Disabling Use Of Cache")
                    configuration.useCache = false
                }else{
                    println("Loading Caching Data")
                    inboxList.clear()
                    inboxList.addAll(localStorage.inbox)
                    sentList.clear()
                    sentList.addAll(localStorage.sent)
                    draftList.clear()
                    draftList.addAll(localStorage.draft)
                    trashList.clear()
                    trashList.addAll(localStorage.trash)
                }
            }

            val matchingMessages = Collections.synchronizedList(ArrayList<Message>())

            val tokenMessenger = SecureMessenger.resolveViaServiceCode(serviceCode)
            val tokenGeneratinCredentials = Credentials(username, password)
            tokenMessenger.login(tokenGeneratinCredentials)

            val inboxToken = tokenMessenger.getAuthenticationToken(1)
            val sentToken = tokenMessenger.getAuthenticationToken(1)
            val draftToken = tokenMessenger.getAuthenticationToken(1)
            val trashToken = tokenMessenger.getAuthenticationToken(1)

            val executor = Executors.newFixedThreadPool(20)

            println("Spawning Thread For Searching Inbox")
            //spawn in new thread

            val inboxThread = thread{

                if(useCache){
                    inboxList.forEach{
                        if(Searcher.messageCointainsKey(it, query)){
                            matchingMessages.add(it)
                        }
                    }
                }else{
                    val messenger = SecureMessenger.resolveViaServiceCode(serviceCode)
                    val credentals = Credentials(inboxToken)
                    messenger.login(credentals)

                    //fetch all inbox messages
                    val inboxSearchFilter = SearchMessagesFilter()
                    inboxSearchFilter.messageBoxType = MessageBoxType.INBOX
                    inboxSearchFilter.pageSize = 100
                    val inboxSearchResults = messenger.searchMessages(inboxSearchFilter)

                    println("Spawning Inbox Executor")
                    val iterator = inboxSearchResults.iterator()
                    while(iterator.hasNext()){
                        val message = iterator.next()

                        executor.execute{
                            val retrievedMessage =  messenger.getMessage(message.messageGuid)

                            if(cacheResults){
                                inboxList.add(retrievedMessage)
                            }

                            if(Searcher.messageCointainsKey(retrievedMessage, query)){
                                matchingMessages.add(retrievedMessage)
                            }

                            return@execute
                        }
                    }
                }
                println("Inbox Thread Complete")
                return@thread
            }

            println("Spawning Thread For Searching Sent")
            //spawn in new thread
            val sentThread = thread{

                if(useCache){
                    sentList.forEach{
                        if(Searcher.messageCointainsKey(it, query)){
                            matchingMessages.add(it)
                        }
                    }
                }else{
                    val messenger = SecureMessenger.resolveViaServiceCode(serviceCode)
                    val credentals = Credentials(sentToken)
                    messenger.login(credentals)

                    //fetch all sent messages
                    val sentSearchFilter = SearchMessagesFilter()
                    sentSearchFilter.messageBoxType = MessageBoxType.SENT
                    sentSearchFilter.pageSize = 100
                    val sentSearchResults = messenger.searchMessages(sentSearchFilter)

                    println("Spawning Sent Executor")
                    //val executor = Executors.newFixedThreadPool(5)

                    val iterator = sentSearchResults.iterator()
                    while(iterator.hasNext()){
                        val message = iterator.next()

                        executor.execute{
                            val retrievedMessage =  messenger.getMessage(message.messageGuid)

                            if(cacheResults){
                                sentList.add(retrievedMessage)
                            }

                            if(Searcher.messageCointainsKey(retrievedMessage, query)){
                                matchingMessages.add(retrievedMessage)
                            }

                            return@execute
                        }
                    }
                }

                println("Sent Thread Complete")
                return@thread
            }

            println("Spawning Thread For Searching Drafts")
            //spawn in new thread
            val draftThread = thread{

                if(useCache){
                    draftList.forEach{
                        if(Searcher.messageCointainsKey(it, query)){
                            matchingMessages.add(it)
                        }
                    }
                }else{
                    val messenger = SecureMessenger.resolveViaServiceCode(serviceCode)
                    val credentals = Credentials(draftToken)
                    messenger.login(credentals)

                    //fetch all draft messages
                    val draftSearchFilter = SearchMessagesFilter()
                    draftSearchFilter.messageBoxType = MessageBoxType.DRAFT
                    draftSearchFilter.pageSize = 100
                    val draftSearchResults = messenger.searchMessages(draftSearchFilter)

                    println("Spawning Draft Executor")

                    val iterator = draftSearchResults.iterator()
                    while(iterator.hasNext()){
                        val message = iterator.next()

                        executor.execute{
                            val retrievedMessage =  messenger.getMessage(message.messageGuid)

                            if(cacheResults){
                                draftList.add(retrievedMessage)
                            }

                            if(Searcher.messageCointainsKey(retrievedMessage, query)){
                                matchingMessages.add(retrievedMessage)
                            }

                            return@execute
                        }
                    }
                }
                println("Draft Thread Complete")
                return@thread
            }


            println("Spawning Thread For Searching Trash")
            //spawn in new thread
            val trashThread = thread{

                if(useCache){
                    trashList.forEach{
                        if(Searcher.messageCointainsKey(it, query)){
                            matchingMessages.add(it)
                        }
                    }
                }else{
                    val messenger = SecureMessenger.resolveViaServiceCode(serviceCode)
                    val credentals = Credentials(trashToken)
                    messenger.login(credentals)

                    //fetch all trash messages
                    val trashSearchFilter = SearchMessagesFilter()
                    trashSearchFilter.messageBoxType = MessageBoxType.TRASH
                    trashSearchFilter.pageSize = 100
                    val trashSearchResults = messenger.searchMessages(trashSearchFilter)

                    println("Spawning Trash Executor")

                    val iterator = trashSearchResults.iterator()
                    while(iterator.hasNext()){
                        val message = iterator.next()

                        executor.execute{
                            val retrievedMessage =  messenger.getMessage(message.messageGuid)

                            if(cacheResults){
                                trashList.add(retrievedMessage)
                            }

                            if(Searcher.messageCointainsKey(retrievedMessage, query)){
                                matchingMessages.add(retrievedMessage)
                            }

                            return@execute
                        }
                    }
                }

                println("Trash Thread Complete")
                return@thread
            }

            println("Waiting For Thread Termination")
            //wait for all threads to finish
            inboxThread.join()
            sentThread.join()
            draftThread.join()
            trashThread.join()

            //once all these threads have terminated, we know there are no more items being added to the executor
            println("Thread Termination Complete. Waiting On Executor Shutdown")
            executor.shutdown()
            while(!executor.isShutdown && !executor.isTerminated){
                //wait for stuff to finish
            }
            executor.shutdownNow()

            println("Executor Shutdown Complete. Closing API Session")
            tokenMessenger.deleteAuthenticationToken(inboxToken)
            tokenMessenger.deleteAuthenticationToken(sentToken)
            tokenMessenger.deleteAuthenticationToken(draftToken)
            tokenMessenger.deleteAuthenticationToken(trashToken)

            if(configuration.cacheResults){
                println("Cache Results Detected. Caching Data")
                val localStorage = LocalStorage(inboxList, sentList, draftList, trashList)
                CacheManager().cacheData(localStorage, serviceCode)
            }

            return SearchResults(matchingMessages, inboxList, draftList, sentList, trashList)

        }
    }

}